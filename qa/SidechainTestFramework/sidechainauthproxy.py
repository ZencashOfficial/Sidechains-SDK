try:
    import http.client as httplib
except ImportError:
    import httplib
import base64
import decimal
import json
import logging
try:
    import urllib.parse as urlparse
except ImportError:
    import urlparse

USER_AGENT = "SidechainAuthServiceProxy/0.1"

HTTP_TIMEOUT = 6000000

class SCAPIException(Exception):
    def __init__(self, sc_api_error):
        Exception.__init__(self)
        self.error = sc_api_error

class SidechainAuthServiceProxy(object):
    __id_count = 0

    def __init__(self, service_url, service_name=None, timeout=HTTP_TIMEOUT, connection=None):
        self.__service_url = service_url
        self.__service_name = service_name
        self.__url = urlparse.urlparse(service_url)
        if self.__url.port is None:
            port = 80
        else:
            port = self.__url.port
        (user, passwd) = (self.__url.username, self.__url.password)
        try:
            user = user.encode('utf8')
        except AttributeError:
            pass
        try:
            passwd = passwd.encode('utf8')
        except AttributeError:
            pass
        #Will we have authentication for SC API too ?
        authpair = user + b':' + passwd
        self.__auth_header = b'Basic ' + base64.b64encode(authpair)

        if connection:
            # Callables re-use the connection of the original proxy
            self.__conn = connection
        elif self.__url.scheme == 'https':
            self.__conn = httplib.HTTPSConnection(self.__url.hostname, port,
                                                  None, None, False,
                                                  timeout)
        else:
            self.__conn = httplib.HTTPConnection(self.__url.hostname, port,
                                                 False, timeout)

    def __getattr__(self, name):
        if name.startswith('__') and name.endswith('__'):
            # Python internal stuff
            raise AttributeError
        if self.__service_name is not None:
            name = "%s.%s" % (self.__service_name, name)
        return SidechainAuthServiceProxy(self.__service_url, name, connection=self.__conn)

    def _request(self, method, path, postdata):
        '''
        Do a HTTP request, with retry if we get disconnected (e.g. due to a timeout).
        This is a workaround for https://bugs.python.org/issue3566 which is fixed in Python 3.5.
        '''
        
        headers = {'Host': self.__url.hostname,
                   'User-Agent': USER_AGENT,
                   'Authorization': self.__auth_header,
                   'Content-type': 'application/json'}
        try:
            self.__conn.request(method, path, postdata, headers)
            return self._get_response()
        except Exception as e:
            # If connection was closed, try again.
            # Python 3.5+ raises BrokenPipeError instead of BadStatusLine when the connection was reset.
            # ConnectionResetError happens on FreeBSD with Python 3.4.
            # These classes don't exist in Python 2.x, so we can't refer to them directly.
            if ((isinstance(e, httplib.BadStatusLine) and e.line == "''")
                or e.__class__.__name__ in ('BrokenPipeError', 'ConnectionResetError')):
                self.__conn.close()
                self.__conn.request(method, path, postdata, headers)
                return self._get_response()
            else:
                raise

    def __call__(self, *args):
        SidechainAuthServiceProxy.__id_count += 1
        path = "/" + self.__service_name.replace("_","/")
        postdata = None
        if len(args) > 0:
            postdata = args[0]
        response = self._request("POST", path, postdata)
        return response

    def _get_response(self):
        http_response = self.__conn.getresponse()
        if http_response is None:
            raise SCAPIException("missing HTTP response from server")
        responsedata = http_response.read().decode('utf8')
        if http_response.status is not 200: #For the moment we check for errors in this way
            raise SCAPIException(responsedata)
        response = json.loads(responsedata, parse_float=decimal.Decimal)
        return response