package com.rasel.androidbaseapp.ui.gallery

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingData
import com.rasel.androidbaseapp.base.BaseFragment
import com.rasel.androidbaseapp.databinding.FragmentGalleryBinding
import com.rasel.androidbaseapp.presentation.viewmodel.BaseViewModel
import com.rasel.androidbaseapp.ui.settings.SettingsFragment
import com.rasel.androidbaseapp.util.asMergedLoadStates
import com.rasel.androidbaseapp.util.toJsonString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class GalleryFragment : BaseFragment<FragmentGalleryBinding, BaseViewModel>() {

    private lateinit var adapter: GalleryAdapter

    //    private val args: GalleryFragmentArgs by navArgs()
    private var searchJob: Job? = null

    override fun getViewBinding(): FragmentGalleryBinding =
        FragmentGalleryBinding.inflate(layoutInflater)

    override val viewModel: GalleryViewModel by viewModels()
    private val searchRequestViewModel: SearchRequestViewModel by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = GalleryAdapter {
            val action = GalleryFragmentDirections.actionNavGalleryToGridFragment()
            findNavController().navigate(action)
        }


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { adapter.refresh() }

        binding.recycleView.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PostsLoadStateAdapter(adapter),
            footer = PostsLoadStateAdapter(adapter)
        )

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(
            SettingsFragment.MY_KEY
        )?.observe(viewLifecycleOwner) {
            // get your result here
            // show Dialog B again if you like ?

            Timber.tag("rsl").d("result value : $it")

            if (it) {
                searchRequestViewModel.shouldWaitForNewUpdate = true
                val action = GalleryFragmentDirections.actionNavGalleryToDialogInsurancePolicy()
                findNavController().navigate(action)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.CREATED)
                // Use a state-machine to track LoadStates such that we only transition to
                // NotLoading from a RemoteMediator load if it was also presented to UI.
                .asMergedLoadStates()
                // Only emit when REFRESH changes, as we only want to react on loads replacing the
                // list.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where REFRESH completes i.e., NotLoading.
//                .filter { it.refresh is LoadState.Error }
                // Scroll to top is synchronous with UI updates, even if remote load was triggered.
                .collect { loadState ->
//                    binding.photoList.scrollToPosition(0)

                    when (loadState.refresh) {
                        is LoadState.Error -> {
                            adapter.submitData(lifecycle, PagingData.empty())
                            binding.tvNoDataFound.isVisible = true
                            binding.progressCircular.isVisible = false
                        }

                        is LoadState.Loading -> {
                            binding.tvNoDataFound.isVisible = false
                            binding.progressCircular.isVisible = true
                        }

                        else -> {
                            binding.tvNoDataFound.isVisible = false
                            binding.progressCircular.isVisible = false

                        }
                    }
                    Timber.tag("rsl").d("paging loadState: ${loadState.toJsonString()}")

                }

            /*adapter.addLoadStateListener { loadState ->
                if (loadState.source.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached && adapter.itemCount < 1) {
                    binding.recycleView.isVisible = false
                    binding.tvNoDataFound.isVisible = true
                } else {
                    binding.recycleView.isVisible = true
                    binding.tvNoDataFound.isVisible = false
                }

                Timber.tag("rsl").d("paging loadState: ${loadState.toJsonString()}")
            }*/
        }

        searchRequestViewModel.searchQuery.observe(viewLifecycleOwner) {
            binding.btnCurrentQuery.text = it.search
            //        search(args.plantName)
            if (adapter.itemCount == 0 || !searchRequestViewModel.shouldWaitForNewUpdate) {
                search(it.search)
                initSearch()
            }
        }

        binding.btnCurrentQuery.setOnClickListener {
            searchRequestViewModel.cloneSearchQuery.value = searchRequestViewModel.searchQuery.value

            val action = GalleryFragmentDirections.actionNavGalleryToDialogInsurancePolicy()
            findNavController().navigate(action)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.CREATED)
                .collect { loadStates ->
                    binding.swipeRefresh.isRefreshing =
                        loadStates.mediator?.refresh is LoadState.Loading
                }
        }

    }

    private fun initSearch() {
        binding.input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                updatedSubredditFromInput()
                true
            } else {
                false
            }
        }
        binding.input.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updatedSubredditFromInput()
                true
            } else {
                false
            }
        }
    }

    private fun updatedSubredditFromInput() {
        binding.input.text.trim().toString().let {
            if (it.isNotBlank()) {
                search(it)
            }
        }
    }

    private fun search(query: String) {
        // Make sure we cancel the previous job before creating a new one
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            viewModel.searchPictures(query).collectLatest {
                adapter.submitData(it)
            }
        }
    }
}
