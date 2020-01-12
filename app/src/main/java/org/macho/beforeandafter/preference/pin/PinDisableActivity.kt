package org.macho.beforeandafter.preference.pin

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_pin.*
import org.macho.beforeandafter.R
import org.macho.beforeandafter.shared.base.BasePinActivity
import org.macho.beforeandafter.shared.util.LogUtil
import org.macho.beforeandafter.shared.util.SharedPreferencesUtil

open class PinDisableActivity: BasePinActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pinTitle.text = getString(R.string.pin_enable_title)
        pinMessage.text = getString(R.string.pin_enable_message)
    }

    override fun completeInput() {
        disablePINIfNeeded()
    }

    private fun disablePINIfNeeded() {
        val pin = SharedPreferencesUtil.getString(this, SharedPreferencesUtil.Key.PIN)
        LogUtil.d(this, "validpin:${pin}")
        if (pin.equals(hiddenEditText.text.toString())) {
            pinMessage.text = getString(R.string.pin_disable_message_ok)
            SharedPreferencesUtil.setString(this, SharedPreferencesUtil.Key.PIN, "")
            setResult(RESULT_OK)
            finish()
        } else {
            pinMessage.text = getString(R.string.pin_auth_message_ng)
            clear()
        }
    }
}