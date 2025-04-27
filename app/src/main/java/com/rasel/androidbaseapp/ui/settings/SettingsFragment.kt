package com.rasel.androidbaseapp.ui.settings

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.util.Pair
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rasel.androidbaseapp.R
import com.rasel.androidbaseapp.base.DownloadFragment
import com.rasel.androidbaseapp.core.theme.ThemeUtils
import com.rasel.androidbaseapp.data.models.Localization
import com.rasel.androidbaseapp.databinding.FragmentSettingsBinding
import com.rasel.androidbaseapp.presentation.viewmodel.BaseViewModel
import com.rasel.androidbaseapp.presentation.viewmodel.LocalizedViewModel
import com.rasel.androidbaseapp.presentation.viewmodel.SettingUIModel
import com.rasel.androidbaseapp.presentation.viewmodel.SettingsViewModel
import com.rasel.androidbaseapp.ui.dialog.BankData
import com.rasel.androidbaseapp.ui.dialog.DialogForBank
import com.rasel.androidbaseapp.ui.image_slider.GridFragmentDirections
import com.rasel.androidbaseapp.ui.image_slider.ImageSliderFragmentDirections
import com.rasel.androidbaseapp.ui.scrolling_tab.WithScrollViewFragmentDirections
import com.rasel.androidbaseapp.util.FullScreenBottomSheetDialog
import com.rasel.androidbaseapp.util.TimeUtils
import com.rasel.androidbaseapp.util.TimeUtils.getStringDateFromTimeInMillis
import com.rasel.androidbaseapp.util.observe
import com.rasel.androidbaseapp.util.result.EventObserver
import com.rasel.androidbaseapp.util.toast
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : DownloadFragment<FragmentSettingsBinding, BaseViewModel>(),
    Toolbar.OnMenuItemClickListener {

    override val viewModel: SettingsViewModel by viewModels()
    private val localizedViewModel: LocalizedViewModel by activityViewModels()

    @Inject
    lateinit var settingsAdapter: SettingsAdapter

    @Inject
    lateinit var themeUtils: ThemeUtils

    private lateinit var dialogForBank: DialogForBank

    private lateinit var dateRangePicker: MaterialDatePicker<Pair<Long, Long>>
    private var endDate: String = ""
    private var startDate: String = ""

    private lateinit var datePicker: MaterialDatePicker<Long>
    private var selectedDate: String = ""

    override fun getViewBinding(): FragmentSettingsBinding =
        FragmentSettingsBinding.inflate(layoutInflater)



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.toolbar.inflateMenu(R.menu.main)
        binding.toolbar.setOnMenuItemClickListener(this)

//        registerDownloadReceiver()
        manageDeepLink()

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(MY_KEY)?.observe(viewLifecycleOwner) {
            // get your result here
            // show Dialog B again if you like ?

            Timber.tag("rsl").d("result value : $it")

            if(it){
                val action = SettingsFragmentDirections.actionNavSettingsToDialogInsurancePolicy()
                findNavController().navigate(action)
            }
        }

        val sentences = listOf(
            "The quick brown fox jumps over the lazy dog.",
            "Lorem ipsum dolor sit amet, consecrate disciplining elite.",
            "This is a random sentence.",
            "Kotlin is a modern programming language.",
            "Artificial Intelligence is shaping the future."
        )

        val bankDataList = mutableListOf<BankData>()

        for (i in 1..40) {
            bankDataList.add(BankData(i, sentences.random()))
        }


        dialogForBank = DialogForBank.display(bankDataList, ::addItem)


        localizedViewModel.localizationFlow.asLiveData().observe(viewLifecycleOwner) {
            binding.btnEnglish.text = it.lblEnglish
            binding.btnBurmese.text = it.lblBurmese
            binding.btnChinese.text = it.lblChinese

            binding.textView.text =
                Localization.getTemplatedString(
                    it.lblSelectedLanguage,
                    localizedViewModel.currentLanguageFlow.value
                )

        }
        binding.btnEnglish.setOnClickListener { localizedViewModel.switchToEnglish() }
        binding.btnBurmese.setOnClickListener { localizedViewModel.switchToBurmese() }
        binding.btnChinese.setOnClickListener { localizedViewModel.switchToChinese() }

        binding.chipHome.setOnClickListener {
            findNavController().navigate(R.id.action_global_nav_home)
        }
        binding.chipOwl.setOnClickListener {
            findNavController().navigate(R.id.owl_graph)
        }
        binding.chipCounter.setOnClickListener {
            findNavController().navigate(R.id.action_global_counter_fragment)
        }

        binding.chipFullScreenBottomSheet.setOnClickListener {
            FullScreenBottomSheetDialog.display(
                childFragmentManager, "history",
                "it1"
            )
        }
        binding.chipFullscreenDialog.setOnClickListener {
            dialogForBank.show(
                childFragmentManager, "history",
            )
        }
        binding.btnDownload.setOnClickListener {
            checkPermissionAndDownload()
        }
        binding.chipBottomSheet.setOnClickListener {
            /*val dialog = DialogInsurancePolicy {
                Toast.makeText(requireContext(), "Clicked", Toast.LENGTH_SHORT).show()
            }
            val args = Bundle()
            args.putBoolean("is_update", false)
            dialog.arguments = args
            dialog.show(parentFragmentManager, "insurance_dialog")*/

            val action = SettingsFragmentDirections.actionNavSettingsToDialogInsurancePolicy()
            findNavController().navigate(action)
        }
        setDateRangeSelection()
        setDateSelection()

        val adapterOnHold = ArrayAdapter(
            requireContext(),
            R.layout.list_item,
            resources.getStringArray(R.array.hold_reasons_v2)
        )
        adapterOnHold.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        (binding.tilReasonList.editText as? AutoCompleteTextView)?.setAdapter(adapterOnHold)

        observe(viewModel.settings, ::onViewStateChange)
        setupRecyclerView()
        viewModel.getSettings()

        viewModel.navigateToThemeSelector.observe(viewLifecycleOwner, EventObserver {
            ThemeSettingDialogFragment.newInstance()
                .show(parentFragmentManager, null)
        })
        viewModel.navigateToLanguageSelector.observe(viewLifecycleOwner, EventObserver {
            LanguageSettingDialogFragment.newInstance()
                .show(parentFragmentManager, null)
        })

        dfdk()
    }

    private fun manageDeepLink() {
        val data: Uri? = requireActivity().intent?.data

        data?.let { uri ->
            context?.toast(uri.toString())

            val keyword = uri.getQueryParameter("keyword")
            val checkInDate = uri.getQueryParameter("checkInDate")
            val checkOutDate = uri.getQueryParameter("checkOutDate")
            val numberOfRooms = uri.getQueryParameter("numberOfRooms")
            val numberOfAdultTravelers = uri.getQueryParameter("numberOfAdultTravelers")
            val numberOfAdultChildren = uri.getQueryParameter("numberOfAdultChildren")
            val productType = uri.getQueryParameter("productType")
            val countryCode = uri.getQueryParameter("countryCode")
            val latitude = uri.getQueryParameter("latitude")
            val longitude = uri.getQueryParameter("longitude")
            val searchId = uri.getQueryParameter("searchId")
            val searchType = uri.getQueryParameter("searchType")

            val segments = uri.pathSegments
            val userName = segments.getOrNull(0)   // "rasel"
            val cityName = segments.getOrNull(1)    // "dhaka"
            val propertySlug = segments.getOrNull(2) // "white-house-guest-house-712313"

            Timber.tag("rsl").d( """
            Original URL: $uri
            keyword = $keyword
            checkInDate = $checkInDate
            checkOutDate = $checkOutDate
            numberOfRooms = $numberOfRooms
            numberOfAdultTravelers = $numberOfAdultTravelers
            numberOfAdultChildren = $numberOfAdultChildren
            productType = $productType
            countryCode = $countryCode
            latitude = $latitude
            longitude = $longitude
            searchId = $searchId
            searchType = $searchType
            userName (path) = $userName
            cityName (path) = $cityName
            propertySlug (path) = $propertySlug
        """.trimIndent())
        }
    }

    fun generateDeepLinkUrl(
        domain: String,
        city: String,
        propertySlug: String,
        keyword: String? = null,
        checkInDate: String? = null,
        checkOutDate: String? = null,
        numberOfRooms: Int? = null,
        numberOfAdultTravelers: Int? = null,
        numberOfAdultChildren: Int? = null,
        productType: Int? = null,
        countryCode: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        searchId: Int? = null,
        searchType: String? = null
    ): String {
        val baseUrl = "$domain/$city/$propertySlug"

        val queryParams = mutableListOf<String>()

        keyword?.let { queryParams.add("keyword=${Uri.encode(it)}") }
        checkInDate?.let { queryParams.add("checkInDate=$it") }
        checkOutDate?.let { queryParams.add("checkOutDate=$it") }
        numberOfRooms?.let { queryParams.add("numberOfRooms=$it") }
        numberOfAdultTravelers?.let { queryParams.add("numberOfAdultTravelers=$it") }
        numberOfAdultChildren?.let { queryParams.add("numberOfAdultChildren=$it") }
        productType?.let { queryParams.add("productType=$it") }
        countryCode?.let { queryParams.add("countryCode=$it") }
        latitude?.let { queryParams.add("latitude=$it") }
        longitude?.let { queryParams.add("longitude=$it") }
        searchId?.let { queryParams.add("searchId=$it") }
        searchType?.let { queryParams.add("searchType=$it") }

        return if (queryParams.isNotEmpty()) {
            "$baseUrl?${queryParams.joinToString("&")}"
        } else {
            baseUrl
        }
    }

    private fun dfdk(){

        binding.fab.setOnClickListener {
            val dialog: AppCompatDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Dialog Title")
                .setMessage("This is the dialog content.")
                .setPositiveButton("OK") { dialog, which ->
                    // Handle positive button click
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    // Handle negative button click
                    dialog.dismiss()
                }
                .create()

            // Optional: Set anchor view for bottom sheet like behavior
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                dialog.behavior = BottomSheetBehavior.from(dialog.window.decorView.findViewById(R.id.content))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bottomSheetBehavior = dialog.getBehavior<BottomSheetBehavior<View>>()
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.peekHeight = peekHeight // Optional: Set peek height for bottom sheet
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED // Optional: Set initial state
                }
            }*/


            dialog.show()
        }

    }

    // when activity has toolbar
    /*override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.main, menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.action_logOut -> {
                logOutFromApp(); true
            }

            R.id.action_settings -> {
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }*/

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_logOut -> {
                logOutFromApp(); true
            }

            R.id.action_settings -> {
                val action = GridFragmentDirections.actionGlobalGridFragment()
                findNavController().navigate(action)
                return true
            }

            R.id.action_image_slider -> {
                val action = ImageSliderFragmentDirections.actionGlobalNavImageSliderFragment()
                findNavController().navigate(action)
                return true
            }

            R.id.action_scroll_view -> {
                val action = WithScrollViewFragmentDirections.actionGlobalWithScrollViewFragment()
                findNavController().navigate(action)
                return true
            }

            R.id.action_fag -> {
                val action = SettingsFragmentDirections.actionNavSettingsToNavFaq()
                findNavController().navigate(action)
                return true
            }

            else -> false
        }
    }


    private fun logOutFromApp() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.titleLogOut))
            .setMessage(resources.getString(R.string.messageLogOut))
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->
                // Respond to negative button press
                dialog.dismiss()
            }
            .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->
                // Respond to positive button press
                dialog.dismiss()
                activity?.finish()
            }
            .show()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewSettings.apply {
            adapter = settingsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        settingsAdapter.setItemClickListener { selectedSetting ->
            when (selectedSetting.id) {
                3 -> {
                    viewModel.onThemeSettingClicked()
                }

                4 -> {
                    viewModel.onLanguageSettingClicked()
                }

                else -> {
                    viewModel.setSettings(selectedSetting)
                }
            }
            Timber.d(selectedSetting.settingLabel)
        }
    }

    private fun setDateRangeSelection() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = calendar.timeInMillis
        dateRangePicker = TimeUtils.getDateRangePicker()
            .setSelection(
                Pair(yesterday, MaterialDatePicker.todayInUtcMilliseconds())
            ).build()

        dateRangePicker.addOnPositiveButtonClickListener {
            parseSelectedDate(it)
        }
        dateRangePicker.selection?.let { parseSelectedDate(it) }

        binding.chipSelectedDateRange.setOnClickListener {
            if (!dateRangePicker.isAdded) {
                dateRangePicker.show(
                    parentFragmentManager,
                    "datePicker_courier_send_report"
                )
            }
        }
    }

    private fun setDateSelection() {
        datePicker =
            TimeUtils.getDatePicker(isTodaySelected = true, fromNow = true, futureDate = 15)
        datePicker.addOnPositiveButtonClickListener {
            selectedDate = getStringDateFromTimeInMillis(it, "yyyy-MM-dd")
            binding.tilDateSelection.editText?.setText(
                getStringDateFromTimeInMillis(
                    it,
                    "dd MMM yyyy"
                )
            )
        }
        binding.tilDateSelection.editText?.setOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(parentFragmentManager, "datePicker_on_hold")
            }
        }
    }

    private fun parseSelectedDate(it: Pair<Long, Long>) {
        startDate = getStringDateFromTimeInMillis(it.first, "yyyy-MM-dd")
        endDate = getStringDateFromTimeInMillis(it.second, "yyyy-MM-dd")

        val dateText = "${
            getStringDateFromTimeInMillis(
                it.first,
                "dd MMM"
            )
        } to ${getStringDateFromTimeInMillis(it.second, "dd MMM")}"
        binding.chipSelectedDateRange.text = dateText
    }

    private fun onViewStateChange(result: SettingUIModel) {
        if (result.isRedelivered) return
        when (result) {
            is SettingUIModel.Error -> handleErrorMessage(result.error)
            SettingUIModel.Loading -> handleLoading(true)
            is SettingUIModel.NightMode -> {
                result.nightMode.let {
                    themeUtils.setNightMode(it)
                }
            }

            is SettingUIModel.Success -> {
                handleLoading(false)
                result.data.let {
                    settingsAdapter.list = it
                }
            }
        }
    }

    private fun addItem(bankData: BankData?) {
        Toast.makeText(requireContext(), bankData?.bankTitle, Toast.LENGTH_SHORT).show()
        dialogForBank.dismiss()
    }
    // mnopqrs

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()

        const val MY_KEY = "result_check"

        private const val TAG = "rsl"
    }


}