package com.shoppilist.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.shoppilist.shared.resources.*
import com.shoppilist.shared.domain.AppLanguage
import com.shoppilist.shared.domain.Country
import com.shoppilist.shared.domain.CountryLanguageData
import com.shoppilist.shared.presentation.OnboardingViewModel

/** Country selection (§2.1) — first onboarding step, before language and before auth. */
@Composable
fun CountrySelectionScreen(onCountrySelected: (Country) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(Res.string.title_select_country), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        CountryPickerList(modifier = Modifier.weight(1f), onSelect = onCountrySelected)
    }
}

/** Alphabetically-sorted, search-filtered country list — the CSV catalog now covers 19
 *  countries, too many for a plain unfiltered list. Reused by onboarding and Settings. */
@Composable
fun CountryPickerList(modifier: Modifier = Modifier, onSelect: (Country) -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search country") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        val filtered = remember(query) {
            CountryLanguageData.countries
                .filter { it.name.contains(query, ignoreCase = true) }
                .sortedBy { it.name }
        }
        LazyColumn {
            items(filtered, key = { it.code }) { country ->
                ListItem(
                    leadingContent = { Text(country.flag, style = MaterialTheme.typography.headlineSmall) },
                    headlineContent = { Text(country.name) },
                    modifier = Modifier.clickable { onSelect(country) }
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
        Text(stringResource(Res.string.title_select_language), style = MaterialTheme.typography.headlineSmall)
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
