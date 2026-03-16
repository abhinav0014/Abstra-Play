package com.streamsphere.app.data.dlna

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jupnp.UpnpService
import org.jupnp.UpnpServiceImpl
import org.jupnp.controlpoint.ControlPoint
import org.jupnp.model.meta.RemoteDevice
import org.jupnp.registry.DefaultRegistryListener
import org.jupnp.registry.Registry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DlnaRepository @Inject constructor() {

    companion object {
        private const val TAG = "DlnaRepository"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var upnpService: UpnpService? = null

    private val _devices = MutableStateFlow<List<DlnaDevice>>(emptyList())
    val devices: StateFlow<List<DlnaDevice>> = _devices.asStateFlow()

    private val _isBound = MutableStateFlow(false)
    val isBound: StateFlow<Boolean> = _isBound.asStateFlow()

    private val registryListener = object : DefaultRegistryListener() {
        override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
            Log.d(TAG, "Device added: ${device.details?.friendlyName}")
            refreshDevices(registry)
        }

        override fun remoteDeviceUpdated(registry: Registry, device: RemoteDevice) {
            refreshDevices(registry)
        }

        override fun remoteDeviceRemoved(registry: Registry, device: RemoteDevice) {
            Log.d(TAG, "Device removed: ${device.details?.friendlyName}")
            refreshDevices(registry)
        }
    }

    fun bind() {
        if (_isBound.value) return
        scope.launch {
            try {
                val svc = UpnpServiceImpl()
                svc.startup()
                upnpService = svc
                svc.registry.addListener(registryListener)
                svc.controlPoint.search()
                _isBound.value = true
                Log.d(TAG, "UPnP service started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start UPnP service", e)
            }
        }
    }

    fun unbind() {
        upnpService?.registry?.removeListener(registryListener)
        upnpService?.shutdown()
        upnpService = null
        _isBound.value = false
        _devices.value = emptyList()
        Log.d(TAG, "UPnP service stopped")
    }

    fun search() {
        upnpService?.controlPoint?.search()
    }

    fun getControlPoint(): ControlPoint? = upnpService?.controlPoint

    fun getRemoteDevice(udn: String): RemoteDevice? {
        return upnpService?.registry?.devices
            ?.filterIsInstance<RemoteDevice>()
            ?.firstOrNull { it.identity.udn.identifierString == udn }
    }

    private fun refreshDevices(registry: Registry) {
        _devices.value = registry.devices
            .filterIsInstance<RemoteDevice>()
            .mapNotNull { device -> runCatching { device.toDlnaDevice() }.getOrNull() }
    }
}
