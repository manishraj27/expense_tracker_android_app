package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.*

enum class ExpenseCategory(val emoji: String, val color: Color) {
    ENTERTAINMENT("🎬", Color(0xFF5B8FF9)),
    SHOPPING("🛍", Color(0xFFF7A600)),
    FOOD("🍽", Color(0xFF5AD8A6)),
    TRANSPORT("🚗", Color(0xFFFF6B3B)),
    BILLS("📃", Color(0xFF945FB9)),
    OTHER("📦", Color(0xFF5FB9B9))
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var showAddExpense by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("💰 Expense Tracker") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    },
                    floatingActionButton = {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(start = 30.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            FloatingActionButton(
                                onClick = { showAddExpense = true },
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(Icons.Default.Add, "Add Expense")
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        ExpenseTrackerApp(showAddExpense) { showAddExpense = false }
                    }
                }
            }
        }
    }
}


@Composable
fun ExpenseTrackerApp(showAddExpense: Boolean, onDismissDialog: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var expenses by remember { mutableStateOf(listOf<Expense>()) }

    LaunchedEffect(Unit) {
        expenses = dbHelper.getAllExpenses()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            SummaryCards(expenses)
        }

        item {
            ExpenseChart(expenses)
        }

        item {
            Text(
                "All Expenses 📝",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        items(expenses) { expense ->
            DeletableExpenseItem(
                expense = expense,
                onDelete = {
                    dbHelper.deleteExpense(expense.id)
                    expenses = dbHelper.getAllExpenses()
                }
            )
        }
    }

    if (showAddExpense) {
        AddExpenseDialog(
            onDismiss = onDismissDialog,
            onExpenseAdded = { expense ->
                dbHelper.addExpense(expense)
                expenses = dbHelper.getAllExpenses()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletableExpenseItem(
    expense: Expense,
    onDelete: () -> Unit
) {
    var isTapped by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val category = ExpenseCategory.valueOf(expense.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { isTapped = !isTapped }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.emoji,
                    fontSize = 24.sp,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = expense.date,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (isTapped) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "₹${String.format("%.2f", expense.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete expense",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text(
                    text = "₹${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Delete Expense") },
            text = {
                Text(
                    text = "Are you sure you want to delete this expense?\n" +
                            "Description: ${expense.description}\n" +
                            "Amount: ₹${String.format("%.2f", expense.amount)}"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SummaryCards(expenses: List<Expense>) {
    val totalExpense = expenses.sumOf { it.amount }
    val thisMonthExpenses = expenses.filter {
        val expenseDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(it.date.substring(0, 7))
        val currentDate = Date()
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(expenseDate) ==
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentDate)
    }
    val thisMonthTotal = thisMonthExpenses.sumOf { it.amount }

    Row(
        modifier = Modifier
            .fillMaxWidth()

            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(
            title = "Total Expenses",
            amount = totalExpense,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primaryContainer
        )
        SummaryCard(
            title = "This Month",
            amount = thisMonthTotal,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}

@Composable
fun SummaryCard(title: String, amount: Double, modifier: Modifier = Modifier, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "₹${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(onDismiss: () -> Unit, onExpenseAdded: (Expense) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var description by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense 💸") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = "${selectedCategory.emoji} ${selectedCategory.name}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        ExpenseCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text("${category.emoji} ${category.name}")
                                },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                if (showError) {
                    Text(
                        "Please fill all fields correctly ⚠",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (amountDouble != null && description.isNotEmpty()) {
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val expense = Expense(0, amountDouble, selectedCategory.name, description, currentDate)
                        onExpenseAdded(expense)
                        onDismiss()
                    } else {
                        showError = true
                    }
                }
            ) {
                Text("Add ✅")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel ❌")
            }
        }
    )
}

@Composable
fun ExpenseChart(expenses: List<Expense>) {
    val monthlyExpenses = expenses
        .groupBy { it.date.substring(0, 7) }
        .map { it.value.sumOf { expense -> expense.amount } }
        .takeLast(6)
        .toList()

    // Convert to list of x,y pairs for the chart
    val chartEntryData = monthlyExpenses.mapIndexed { index, value ->
        index.toFloat() to value.toFloat()
    }.toTypedArray()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Chart(
            chart = lineChart(),
            model = entryModelOf(*chartEntryData), // Using spread operator to pass array as vararg
            startAxis = startAxis(),
            bottomAxis = bottomAxis(),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        )
    }
}

