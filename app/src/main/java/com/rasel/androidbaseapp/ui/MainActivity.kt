package com.rasel.androidbaseapp.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.rasel.androidbaseapp.R
import com.rasel.androidbaseapp.databinding.ActivityMain2Binding
import com.rasel.androidbaseapp.presentation.utils.NetworkStatus
import com.rasel.androidbaseapp.presentation.viewmodel.ConnectivityViewModel
import com.rasel.androidbaseapp.presentation.viewmodel.LocalizedViewModel
import com.rasel.androidbaseapp.util.NetworkChangeReceiver
import com.rasel.androidbaseapp.util.NoticeDialogFragment
import com.rasel.androidbaseapp.util.contentView
import com.rasel.androidbaseapp.util.showToast
import com.rasel.androidbaseapp.util.toastSuccess
import com.rasel.androidbaseapp.util.updateForTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NoticeDialogFragment.NoticeDialogListener,
    NetworkChangeReceiver.ConnectionChangeCallback {

    private lateinit var mContext: Context
    private val viewModel: LocalizedViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private val connectivityViewModel: ConnectivityViewModel by viewModels()

    private val binding: ActivityMain2Binding by contentView(R.layout.activity_main2)

    private lateinit var networkChangeReceiver: NetworkChangeReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Update for Dark Mode straight away
        updateForTheme(mainActivityViewModel.currentTheme)

        binding.apply {
            val navController = Navigation.findNavController(this@MainActivity, R.id.nav_host)
            bottomNav.setupWithNavController(navController)

            /* val appBarConfiguration = AppBarConfiguration(
                 setOf(
                     R.id.nav_home,
                     R.id.nav_gallery,
                     R.id.nav_slideshow,
                     R.id.nav_plantListFragment,
                     R.id.nav_character_list,
                     R.id.nav_email_list,
                     R.id.nav_email_list,
                     R.id.nav_settings
                 ), null
             )

             setSupportActionBar(toolbar)
             setupActionBarWithNavController(navController, appBarConfiguration)
             binding.toolbar.setupWithNavController(navController)*/

            // Hide bottom nav on screens which don't require it
            lifecycleScope.launchWhenResumed {
                navController.addOnDestinationChangedListener { controller: NavController, destination: NavDestination, temp: Bundle? ->
                    /*when (destination.id) {
                        R.id.nav_slideshow,
                        R.id.nav_plantListFragment,
                        R.id.nav_character_list,
                        R.id.nav_email_list,
                        R.id.nav_settings -> bottomNav.show()
                        else -> bottomNav.hide()
                    }*/

                    printBackStack(controller)
                }
            }
        }

        mContext = this

        if (savedInstanceState != null) {
            currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0)
            // Return here to prevent adding additional GridFragments when changing orientation.
            return
        }

        mainActivityViewModel.theme.observe(this, Observer(::updateForTheme))

        //network change callback to track network state
        networkChangeReceiver = NetworkChangeReceiver()
        networkChangeReceiver.setConnectionChangeCallback(this)

        connectivityViewModel.networkStatus.observe(this) { networkStatus ->

            if (networkStatus == NetworkStatus.Disconnected) {
                showToast("you are offline")
            }else if (networkStatus == NetworkStatus.Connected) {
                showToast("you are Online")
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_POSITION, currentPosition)
    }

    override fun onStart() {
        super.onStart()
        /* mContext.registerReceiver(
             networkChangeReceiver,
             IntentFilter(NetworkChangeReceiver.ACTION_CONNECTIVITY_CHANGE)
         )*/

        viewModel.getLocalizationFromRemote()
    }

    override fun onStop() {
        super.onStop()
//        mContext.unregisterReceiver(networkChangeReceiver)
    }

    fun showNoticeDialog() {
        // Create an instance of the dialog fragment and show it.
        val dialog = NoticeDialogFragment()
        dialog.show(supportFragmentManager, "NoticeDialogFragment")
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following
    // methods defined by the NoticeDialogFragment.NoticeDialogListener
    // interface.
    override fun onDialogPositiveClick(dialog: DialogFragment) {
        // User taps the dialog's positive button.
        /*Snackbar.make(binding.fab, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()*/
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User taps the dialog's negative button.
        /*Snackbar.make(binding.fab, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()*/
    }

    override fun onConnectionChange(isConnected: Boolean) {
        mContext.toastSuccess("Network isConnected : $isConnected")
    }

    fun printBackStack(navController: NavController) {
        val currentBackStack = navController.currentBackStack.value

        val breadcrumb = currentBackStack.map { it.destination }
            .filterNot { it is NavGraph }
            .joinToString(" > ") { it.displayName.split('/')[1] }

        Timber.tag("rsl").d("Backstack: $breadcrumb")

    }


    companion object {

        /**
         * Holds the current image position to be shared between the grid and the pager fragments. This
         * position updated when a grid item is clicked, or when paging the pager.
         *
         * In this demo app, the position always points to an image index at the [ ] class.
         */
        public var currentPosition = 0
        private const val KEY_CURRENT_POSITION =
            "com.google.samples.gridtopager.key.currentPosition"
    }
}