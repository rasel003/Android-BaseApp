package com.rasel.androidbaseapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.rasel.androidbaseapp.R
import com.rasel.androidbaseapp.presentation.viewmodel.CoroutinesErrorHandler
import com.rasel.androidbaseapp.presentation.viewmodel.MainViewModel
import com.rasel.androidbaseapp.presentation.viewmodel.TokenViewModel
import com.rasel.androidbaseapp.util.ApiResponse
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {
    private val viewModel: MainViewModel by viewModels()
    private val tokenViewModel: TokenViewModel by activityViewModels()

    private lateinit var navController: NavController
    private var token: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        tokenViewModel.token.observe(viewLifecycleOwner) { token ->
            this.token = token
            if (token == null)
                navController.navigate(R.id.action_global_loginFragment)
        }

        val mainTV = view.findViewById<TextView>(R.id.infoTV)
        viewModel.userInfoResponse.observe(viewLifecycleOwner) {
            mainTV.text = when(it) {
                is ApiResponse.Failure -> "Code: ${it.code}, ${it.errorMessage}"
                ApiResponse.Loading -> "Loading"
                is ApiResponse.Success -> "ID: ${it.data.id}\nMail: ${it.data.email}\n\nToken: $token"
            }
        }

        view.findViewById<Button>(R.id.infoButton).setOnClickListener {
            viewModel.getUserInfo(object: CoroutinesErrorHandler {
                override fun onError(message: String) {
                    mainTV.text = "Error! $message"
                }
            })
        }

        view.findViewById<Button>(R.id.logoutButton).setOnClickListener {
            tokenViewModel.deleteToken()
        }
    }
}