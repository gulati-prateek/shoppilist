package com.shoppilist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.shoppilist.R
import com.shoppilist.shared.domain.AppLanguage
import com.shoppilist.shared.domain.Country
import com.shoppilist.shared.domain.CountryLanguageData
import com.shoppilist.shared.presentation.OnboardingViewModel

/** Country selection (§2.1) — first onboarding step, before language and before auth. */
@Composable
fun CountrySelectionScreen(onCountrySelected: (Country) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.title_select_country), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(CountryLanguageData.countries) { country ->
                ListItem(
                    leadingContent = { Text(country.flag, style = MaterialTheme.typography.headlineSmall) },
                    headlineContent = { Text(country.name) },
                    modifier = Modifier.clickable { onCountrySelected(country) }
                )
                HorizontalDivider()
            }
        }
    }
}

/** Language selection, filtered by the previously selected country (§2.1). */
@Composable
fun LanguageSelectionScreen(
    country: Country,
    viewModel: OnboardingViewModel = koinViewModel(),
    onLanguageSelected: () -> Unit
) {
    val languages = remember(country) { CountryLanguageData.languagesFor(country.code) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.title_select_language), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(languages) { language: AppLanguage ->
                ListItem(
                    headlineContent = { Text(language.nativeName) },
                    supportingContent = { Text(language.name) },
                    modifier = Modifier.clickable {
                        viewModel.selectLocale(country.code, language.code)
                        onLanguageSelected()
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
