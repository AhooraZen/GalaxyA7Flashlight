package com.ahoora.a7flashlight.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ahoora.a7flashlight.data.TorchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MasterTorchTileService : TileService() {
    private var listeningScope: CoroutineScope? = null
    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        TorchManager.init(applicationContext)
        updateTileState()

        listeningScope = CoroutineScope(Dispatchers.Main)
        job = listeningScope?.launch {
            launch {
                TorchManager.isRearOn.collect {
                    updateTileState()
                }
            }
            launch {
                TorchManager.isFrontOn.collect {
                    updateTileState()
                }
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        job?.cancel()
        listeningScope = null
    }

    override fun onClick() {
        super.onClick()
        val anyOn = TorchManager.isRearOn.value || TorchManager.isFrontOn.value
        if (anyOn) {
            TorchManager.setBoth(false)
        } else {
            TorchManager.setBoth(true)
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val anyOn = TorchManager.isRearOn.value || TorchManager.isFrontOn.value
        tile.state = if (anyOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
