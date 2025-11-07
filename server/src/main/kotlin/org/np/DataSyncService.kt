package org.np

import java.rmi.Remote
import java.rmi.RemoteException

interface DataSyncService : Remote {
    @Throws(RemoteException::class)
    fun sendData(key: String, value: String): String

    @Throws(RemoteException::class)
    fun replicateChange(key: String, value: String, sourceServerName: String): String
}