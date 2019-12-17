package com.horizen.examples;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import com.horizen.SidechainSettings;
import com.horizen.api.http.ApplicationApiGroup;
import com.horizen.box.*;
import com.horizen.params.MainNetParams;
import com.horizen.proposition.Proposition;
import com.horizen.secret.Secret;
import com.horizen.secret.SecretSerializer;
import com.horizen.settings.SettingsReader;
import com.horizen.storage.IODBStorageUtil;
import com.horizen.storage.Storage;
import com.horizen.state.*;
import com.horizen.transaction.BoxTransaction;
import com.horizen.transaction.TransactionSerializer;
import com.horizen.wallet.*;
import com.horizen.utils.Pair;

public class SimpleAppModule
    extends AbstractModule
{
    private SettingsReader settingsReader;

    public SimpleAppModule(String userSettingsFileName) {
        this.settingsReader = new SettingsReader(userSettingsFileName, Optional.empty());
    }

    @Override
    protected void configure() {

        SidechainSettings sidechainSettings = this.settingsReader.getSidechainSettings();

        HashMap<Byte, BoxSerializer<Box<Proposition>>> customBoxSerializers = new HashMap<>();
        HashMap<Byte, SecretSerializer<Secret>> customSecretSerializers = new HashMap<>();
        HashMap<Byte, TransactionSerializer<BoxTransaction<Proposition, Box<Proposition>>>> customTransactionSerializers = new HashMap<>();

        ApplicationWallet defaultApplicationWallet = new DefaultApplicationWallet();
        ApplicationState defaultApplicationState = new DefaultApplicationState();

        File secretStore = new File(sidechainSettings.scorexSettings().dataDir().getAbsolutePath() + "/secret");
        File walletBoxStore = new File(sidechainSettings.scorexSettings().dataDir().getAbsolutePath() + "/wallet");
        File walletTransactionStore = new File(sidechainSettings.scorexSettings().dataDir().getAbsolutePath() + "/walletTransaction");
        File stateStore = new File(sidechainSettings.scorexSettings().dataDir().getAbsolutePath() + "/state");
        File historyStore = new File(sidechainSettings.scorexSettings().dataDir().getAbsolutePath() + "/history");
        File transactionIndexesStore = new File(sidechainSettings.scorexSettings().dataDir().getAbsolutePath() + "/transactionIndexes");


        // Here I can add my custom rest api and/or override existing one
        List<ApplicationApiGroup> customApiGroups = new ArrayList<>();

        // Here I can reject some of existing API routes
        // Each pair consisto of "group name" -> "route name"
        // For example new Pair("wallet, "allBoxes");
        List<Pair<String, String>> rejectedApiPaths = new ArrayList<>();



        bind(SidechainSettings.class)
                .annotatedWith(Names.named("SidechainSettings"))
                .toInstance(sidechainSettings);

        bind(new TypeLiteral<HashMap<Byte, BoxSerializer<Box<Proposition>>>>() {})
                .annotatedWith(Names.named("CustomBoxSerializers"))
                .toInstance(customBoxSerializers);
        bind(new TypeLiteral<HashMap<Byte, SecretSerializer<Secret>>>() {})
                .annotatedWith(Names.named("CustomSecretSerializers"))
                .toInstance(customSecretSerializers);
        bind(new TypeLiteral<HashMap<Byte, TransactionSerializer<BoxTransaction<Proposition, Box<Proposition>>>>>() {})
                .annotatedWith(Names.named("CustomTransactionSerializers"))
                .toInstance(customTransactionSerializers);

        bind(ApplicationWallet.class)
                .annotatedWith(Names.named("ApplicationWallet"))
                .toInstance(defaultApplicationWallet);

        bind(ApplicationState.class)
                .annotatedWith(Names.named("ApplicationState"))
                .toInstance(defaultApplicationState);

        bind(Storage.class)
                .annotatedWith(Names.named("SecretStorage"))
                .toInstance(IODBStorageUtil.getStorage(secretStore));
        bind(Storage.class)
                .annotatedWith(Names.named("WalletBoxStorage"))
                .toInstance(IODBStorageUtil.getStorage(walletBoxStore));
        bind(Storage.class)
                .annotatedWith(Names.named("WalletTransactionStorage"))
                .toInstance(IODBStorageUtil.getStorage(walletTransactionStore));
        bind(Storage.class)
                .annotatedWith(Names.named("StateStorage"))
                .toInstance(IODBStorageUtil.getStorage(stateStore));
        bind(Storage.class)
                .annotatedWith(Names.named("HistoryStorage"))
                .toInstance(IODBStorageUtil.getStorage(historyStore));
        bind(Storage.class)
                .annotatedWith(Names.named("TransactionIndexesStorage"))
                .toInstance(IODBStorageUtil.getStorage(transactionIndexesStore));

        bind(new TypeLiteral<List<ApplicationApiGroup>> () {})
                .annotatedWith(Names.named("CustomApiGroups"))
                .toInstance(customApiGroups);

        bind(new TypeLiteral<List<Pair<String, String>>> () {})
                .annotatedWith(Names.named("RejectedApiPaths"))
                .toInstance(rejectedApiPaths);
    }
}
