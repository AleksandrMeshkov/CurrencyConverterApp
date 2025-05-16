package com.example.currencyconverterapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.currencyconverterapp.ui.theme.CurrencyConverterAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CurrencyConverterAppTheme {
                MainScreen(context = this)
            }
        }
    }
}

sealed class Screen(val title: String, val icon: ImageVector) {
    object MetalConverter : Screen("Металлы", Icons.Default.MonetizationOn)
    object CurrencyConverter : Screen("Валюты", Icons.Default.CurrencyExchange)
    object History : Screen("История", Icons.Default.History)
}

@Composable
fun MainScreen(context: Context) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.CurrencyConverter) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Screen.MetalConverter.icon, contentDescription = null) },
                    label = { Text(Screen.MetalConverter.title) },
                    selected = currentScreen == Screen.MetalConverter,
                    onClick = { currentScreen = Screen.MetalConverter }
                )
                NavigationBarItem(
                    icon = { Icon(Screen.CurrencyConverter.icon, contentDescription = null) },
                    label = { Text(Screen.CurrencyConverter.title) },
                    selected = currentScreen == Screen.CurrencyConverter,
                    onClick = { currentScreen = Screen.CurrencyConverter }
                )
                NavigationBarItem(
                    icon = { Icon(Screen.History.icon, contentDescription = null) },
                    label = { Text(Screen.History.title) },
                    selected = currentScreen == Screen.History,
                    onClick = { currentScreen = Screen.History }
                )
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            Screen.MetalConverter -> MetalConverterScreen(Modifier.padding(innerPadding))
            Screen.CurrencyConverter -> CurrencyConverterScreen(Modifier.padding(innerPadding), context = context)
            Screen.History -> HistoryScreen(Modifier.padding(innerPadding), context = context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(modifier: Modifier = Modifier, context: Context) {
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("EUR") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var convertedAmount by remember { mutableStateOf("") }

    val currencies = listOf("USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "RUB")

    fun convertCurrency() {
        val amountValue = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
        println("Amount parsed: $amountValue") 

        RetrofitClient.exchangeRateApiService.getExchangeRates(fromCurrency).enqueue(object : Callback<ExchangeRateResponse> {
            override fun onResponse(call: Call<ExchangeRateResponse>, response: Response<ExchangeRateResponse>) {
                if (response.isSuccessful) {
                    val rates = response.body()?.conversion_rates
                    println("API Rates: $rates")
                    val rate = rates?.get(toCurrency) ?: 1.0
                    val convertedValue = amountValue * rate
                    convertedAmount = "%.2f".format(convertedValue)
                    println("Converted value: $convertedValue")

                    val conversion = Conversion(
                        fromCurrency = fromCurrency,
                        toCurrency = toCurrency,
                        amount = amountValue,
                        convertedAmount = convertedValue
                    )

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            db.conversionDao().insert(conversion)
                            println("Saved to DB: $conversion")
                        } catch (e: Exception) {
                            println("DB Error: ${e.message}")
                        }
                    }
                }
            }

            override fun onFailure(
                call: Call<ExchangeRateResponse?>,
                t: Throwable
            ) {
                TODO("Not yet implemented")
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Конвертер валют", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = "Currency Exchange",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Сумма") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = fromExpanded,
                        onExpandedChange = { fromExpanded = it }
                    ) {
                        TextField(
                            value = fromCurrency,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                            label = { Text("Из") },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = fromExpanded,
                            onDismissRequest = { fromExpanded = false }
                        ) {
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        fromCurrency = currency
                                        fromExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Convert",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = toExpanded,
                        onExpandedChange = { toExpanded = it }
                    ) {
                        TextField(
                            value = toCurrency,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                            label = { Text("В") },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = toExpanded,
                            onDismissRequest = { toExpanded = false }
                        ) {
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        toCurrency = currency
                                        toExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { convertCurrency() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Конвертировать", fontSize = 18.sp)
            }

            if (convertedAmount.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Результат",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$amount $fromCurrency = $convertedAmount $toCurrency",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetalConverterScreen(modifier: Modifier = Modifier) {
    var baseCurrency by remember { mutableStateOf("RUB") }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val metals = listOf(
        "XAU" to "Золото",
        "XAG" to "Серебро",
        "XPT" to "Платина",
        "XPD" to "Палладий"
    )

    var metalRates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    val currencies = listOf("RUB", "USD", "EUR", "GBP", "CNY")

    fun fetchMetalRates() {
        isLoading = true
        errorMessage = null

        val metalsCodes = metals.joinToString(",") { it.first }

        MetalRetrofitClient.metalRatesApiService.getMetalRates(
            apiKey = "20225d2e98f80690280df86a362f24ee",
            baseCurrency = baseCurrency,
            currencies = metalsCodes
        ).enqueue(object : Callback<MetalRatesResponse> {
            override fun onResponse(
                call: Call<MetalRatesResponse>,
                response: Response<MetalRatesResponse>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body() != null) {
                    val rates = response.body()!!
                    if (rates.success) {
                        metalRates = metals.associate { (code, name) ->
                            val rateKey = "${baseCurrency}$code"
                            name to (rates.rates[rateKey] ?: 0.0)
                        }
                    } else {
                        errorMessage = "Ошибка в ответе API"
                    }
                } else {
                    errorMessage = "Ошибка сервера: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<MetalRatesResponse>, t: Throwable) {
                isLoading = false
                errorMessage = t.message ?: "Неизвестная ошибка"
            }
        })
    }

    LaunchedEffect(baseCurrency) {
        fetchMetalRates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Курсы металлов", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = isMenuExpanded,
                onExpandedChange = { isMenuExpanded = it }
            ) {
                TextField(
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                    value = baseCurrency,
                    onValueChange = {},
                    label = { Text("Базовая валюта") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMenuExpanded) }
                )

                ExposedDropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    currencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text(currency) },
                            onClick = {
                                baseCurrency = currency
                                isMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(metals.size) { index ->
                    val (code, name) = metals[index]
                    MetalRateCard(
                        title = "$name ($code)",
                        price = "%.2f".format(metalRates[name] ?: 0.0),
                        currency = baseCurrency,
                        icon = Icons.Default.MonetizationOn,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = { fetchMetalRates() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Обновить курсы")
            }
        }
    }
}

@Composable
fun MetalRateCard(
    title: String,
    price: String,
    currency: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "$price $currency/унция",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(modifier: Modifier = Modifier, context: Context) {
    val conversionDao = AppDatabase.getDatabase(context).conversionDao()
    val conversions by conversionDao.getAllConversions().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "История конвертаций",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        content = { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (conversions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Нет данных о конвертациях")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(conversions.size) { index ->
                            ConversionItem(conversion = conversions[index])
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ConversionItem(conversion: Conversion) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "${conversion.amount} ${conversion.fromCurrency} -> ${conversion.convertedAmount} ${conversion.toCurrency}")
            Text(text = "Дата: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(conversion.timestamp))}")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    CurrencyConverterAppTheme {
        MainScreen(context = android.app.Application())
    }
}
