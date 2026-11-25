package com.movil.pagoflex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.movil.pagoflex.ui.screens.pagoflex.AppPagoFlex
import com.movil.pagoflex.viewmodel.AdminViewModel
import com.movil.pagoflex.viewmodel.PagoFlexViewModel

class MainActivity : ComponentActivity() {

    private val vm: PagoFlexViewModel by viewModels { PagoFlexViewModel.factory(application) }
    private val avm: AdminViewModel by viewModels { AdminViewModel.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppPagoFlex(vm, avm)
        }
    }
}
