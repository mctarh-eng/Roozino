package com.roozino.app

import android.app.Application
import com.roozino.app.data.DataRepository
import com.roozino.app.notify.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoozinoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DataRepository.init(this)
        NotificationHelper.createChannels(this)
        // محکم‌کاری: در هر باز شدن اپ، یادآوری‌های آینده دوباره زمان‌بندی می‌شوند
        CoroutineScope(Dispatchers.Default).launch {
            try {
                DataRepository.rescheduleAll()
            } catch (_: Exception) {
            }
        }
    }
}
