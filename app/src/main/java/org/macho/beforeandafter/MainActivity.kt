package org.macho.beforeandafter

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.macho.beforeandafter.alarmsettingdialog.AlarmSettingDialog
import org.macho.beforeandafter.record.editaddrecord.OnRecordSavedListener
import org.macho.beforeandafter.shared.extensions.setupWithNavController
import org.macho.beforeandafter.shared.util.AdUtil
import org.macho.beforeandafter.shared.util.SharedPreferencesUtil
import javax.inject.Inject

class MainActivity: DaggerAppCompatActivity(), OnRecordSavedListener {
    private var currentNavController: LiveData<NavController>? = null

    @Inject
    lateinit var alarmSettingDialog: AlarmSettingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            setUpBottomNavigationBar()
        }

        configureAd()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        setUpBottomNavigationBar()
    }

    private fun setUpBottomNavigationBar() {
        val navGraphIds = listOf(R.navigation.records, R.navigation.gallery, R.navigation.graphe, R.navigation.settings)

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottom_nav.setupWithNavController(
                navGraphIds = navGraphIds,
                fragmentManager = supportFragmentManager,
                containerId = R.id.nav_host_container,
                intent = intent
        )

        // Whenever the selected controller changes, setup the action bar.
        controller.observe(this, Observer { navController ->
            setupActionBarWithNavController(navController)
        })
        currentNavController = controller
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    private fun configureAd() {
        AdUtil.requestConsentInfoUpdateIfNeed(applicationContext)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    // MARK: - OnRecordSavedListener
    override fun onRecordSaved() {
        val isAlarmEnabled = SharedPreferencesUtil.getBoolean(this, SharedPreferencesUtil.Key.ALARM_ENABLED)
        val neverDisplayAlarmSettingDialog = SharedPreferencesUtil.getBoolean(this, SharedPreferencesUtil.Key.NEVER_DISPLAY_ALARM_SETTING_DIALOG)
        if (isAlarmEnabled || neverDisplayAlarmSettingDialog) {
//            return
        }
        alarmSettingDialog.show(supportFragmentManager, null)
    }
}