package org.macho.beforeandafter.preference

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import org.macho.beforeandafter.shared.util.AdUtil
import org.macho.beforeandafter.shared.BeforeAndAfterConst
import org.macho.beforeandafter.R
import org.macho.beforeandafter.shared.data.RecordDao
import java.io.File

class DeleteAllDialog: DialogFragment() {
    companion object {
        fun newInstance(activity: Activity): DialogFragment {
            return DeleteAllDialog()
        }
    }

    private lateinit var interstitialAd: InterstitialAd

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        MobileAds.initialize(activity, getString(R.string.admob_app_id))

        interstitialAd = InterstitialAd(activity)
        AdUtil.loadInterstitialAd(interstitialAd, context!!)

        return AlertDialog.Builder(activity).setTitle(R.string.delete_all_title)
                .setMessage(R.string.delete_all_confirmation_message)
                .setPositiveButton(R.string.ok) { dialogInterface, i ->
                    RecordDao.deleteAll()
                    for (file in File(BeforeAndAfterConst.PATH).listFiles()) {
                        file.delete()
                    }
                    AdUtil.show(interstitialAd)
                }
                .setNegativeButton(R.string.cancel) { dialogInterface, i ->
                    AdUtil.show(interstitialAd)
                }
                .create()
    }
}