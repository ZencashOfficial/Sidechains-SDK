package com.horizen.storage

import java.util.{Optional, ArrayList => JArrayList}

import com.horizen.companion.SidechainBoxesCompanion
import com.horizen.utils.{ByteArrayWrapper, Pair => JPair}
import com.horizen.{OpenedWalletBox, OpenedWalletBoxSerializer, SidechainTypes}
import scorex.crypto.hash.Blake2b256
import scorex.util.ScorexLogging

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class SidechainOpenedWalletBoxStorage(storage : Storage, sidechainBoxesCompanion: SidechainBoxesCompanion)
  extends SidechainTypes
  with ScorexLogging
{
  // Version - block Id
  // Key - byte array box Id
  // No remove operation

  require(storage != null, "Storage must be NOT NULL.")
  require(sidechainBoxesCompanion != null, "SidechainBoxesCompanion must be NOT NULL.")

  private val openedWalletBoxSerializer = new OpenedWalletBoxSerializer(sidechainBoxesCompanion)

  def calculateKey(boxId : Array[Byte]) : ByteArrayWrapper = {
    new ByteArrayWrapper(Blake2b256.hash(boxId))
  }

  def get (boxId : Array[Byte]) : Option[OpenedWalletBox] = {
    storage.get(calculateKey(boxId)) match {
      case v if v.isPresent => {
        openedWalletBoxSerializer.parseBytesTry(v.get().data) match {
          case Success(openedWalletBox) => Option(openedWalletBox)
          case Failure(exception) => {
            log.error("Error while OpenedWalletBox parsing.", exception)
            Option.empty
          }
        }
      }
      case _ => Option.empty
    }
  }

  def getAll : List[OpenedWalletBox] = {
    storage.getAll.asScala.map(baw => {
      openedWalletBoxSerializer.parseBytes(baw.getValue.data)
    }).toList
  }

  def update (version : ByteArrayWrapper,
              openedWalletBoxAppendList : Set[OpenedWalletBox]) : Try[SidechainOpenedWalletBoxStorage] = Try {
    require(openedWalletBoxAppendList != null, "List of OpenedWalletBoxes to append must be NOT NULL. Use empty List instead.")
    require(!openedWalletBoxAppendList.contains(null), "OpenedWalletBox to append must be NOT NULL.")

    val removeList = new JArrayList[ByteArrayWrapper]()
    val updateList = new JArrayList[JPair[ByteArrayWrapper,ByteArrayWrapper]]()

    for (owb <- openedWalletBoxAppendList)
      updateList.add(new JPair[ByteArrayWrapper, ByteArrayWrapper](calculateKey(owb.box.id()),
        new ByteArrayWrapper(openedWalletBoxSerializer.toBytes(owb))))

    storage.update(version,
      updateList,
      removeList)

    this
  }

  def lastVersionId : Optional[ByteArrayWrapper] = {
    storage.lastVersionID()
  }

  def rollbackVersions : List[ByteArrayWrapper] = {
    storage.rollbackVersions().asScala.toList
  }

  def rollback (version : ByteArrayWrapper) : Try[SidechainOpenedWalletBoxStorage] = Try {
    require(version != null, "Version to rollback to must be NOT NULL.")
    storage.rollback(version)
    this
  }

  def isEmpty: Boolean = storage.isEmpty

}
