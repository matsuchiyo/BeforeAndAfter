package org.macho.beforeandafter.preference.backup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.backup_frag.*
import org.macho.beforeandafter.R
import org.macho.beforeandafter.shared.data.RecordRepository
import org.macho.beforeandafter.shared.di.ActivityScoped
import org.macho.beforeandafter.shared.util.AdUtil
import org.macho.beforeandafter.shared.view.AlertDialog
import javax.inject.Inject

@ActivityScoped
class BackupFragment @Inject constructor(): DaggerFragment(), BackupContract.View {
    @Inject
    override lateinit var presenter: BackupContract.Presenter

    @Inject
    lateinit var recordRepository: RecordRepository

    private lateinit var interstitialAd: InterstitialAd

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.backup_frag, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        cancelButton.setOnClickListener { view ->
            presenter.cancelBackup()
            finish()
        }

        finishButton.setOnClickListener { view ->
            finish()
        }

        presenter.takeView(this)
        presenter.backup()

        progressBar.max = 100

        MobileAds.initialize(context, getString(R.string.admob_app_id))

        AdUtil.loadBannerAd(adView, context!!)

        interstitialAd = InterstitialAd(context)
        AdUtil.loadInterstitialAd(interstitialAd, context!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data == null) {
            return
        }
        presenter.result(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.dropView()
    }

    override fun finish() {
        AdUtil.show(interstitialAd)
        activity?.finish()
    }

    override fun setBackupStatusMessageTitle(title: String) {
        activity?.runOnUiThread {
            backupStatusMessageTitle.setText(title)
        }
    }

    override fun setBackupStatusMessageDescription(description: String) {
        activity?.runOnUiThread {
            backupStatusMessageDescription.setText(description)
        }
    }

    override fun setProgress(value: Int) {
        Log.i("BackupFragment", "*** setProgress ***")
        activity?.runOnUiThread {
            progressBar.setProgress(value)
        }
    }

    override fun setFinishButtonEnabled(enabled: Boolean) {
        activity?.runOnUiThread {
            finishButton.isEnabled = enabled
        }
    }

    override fun showAlert(title: String, message: String) {
        activity?.runOnUiThread {
            AlertDialog.newInstance(activity!!, title, message) {
                finish()
            }
        }
    }
}