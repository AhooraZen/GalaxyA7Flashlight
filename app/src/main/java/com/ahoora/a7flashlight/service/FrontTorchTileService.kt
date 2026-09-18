package com.ahoora.a7flashlight.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ahoora.a7flashlight.data.TorchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FrontTorchTileService : TileService() {
    private var listeningScope: CoroutineScope? = null
    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        TorchManager.init(applicationContext)
        updateTileState()

        listeningScope = CoroutineScope(Dispatchers.Main)
        job = listeningScope?.launch {
            TorchManager.isFrontOn.collect {
                updateTileState()
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
        val newState = !TorchManager.isFrontOn.value
        TorchManager.setFrontTorch(newState)
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isOn = TorchManager.isFrontOn.value
        tile.state = if (isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
