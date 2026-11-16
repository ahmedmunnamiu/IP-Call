package com.ahmedmunna.ipcall.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactItem(val name: String, val number: String)

private suspend fun loadContacts(ctx: Context): List<ContactItem> = withContext(Dispatchers.IO) {
    val out = LinkedHashMap<String, ContactItem>()
    try {
        ctx.contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER),
            null, null,
            Phone.DISPLAY_NAME + " COLLATE NOCASE ASC"
        )?.use { c ->
            val ni = c.getColumnIndexOrThrow(Phone.DISPLAY_NAME)
            val pi = c.getColumnIndexOrThrow(Phone.NUMBER)
            while (c.moveToNext()) {
                val raw = c.getString(pi).orEmpty()
                val num = raw.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                if (num.isEmpty()) continue
                val name = c.getString(ni)?.trim().orEmpty().ifBlank { num }
                out.putIfAbsent(name.lowercase() + "|" + num, ContactItem(name, num))
            }
        }
    } catch (_: Throwable) {
    }
    out.values.toList()
}

@Composable
fun ContactsScreen(onCall: (String) -> Unit) {
    val style = LocalGlassStyle.current
    val ctx = LocalContext.current
    val focus: FocusManager = LocalFocusManager.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var denied by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<ContactItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    LaunchedEffect(granted) {
        if (granted) {
            loading = true
            contacts = loadContacts(ctx)
            loading = false
        }
    }

    val filtered = remember(contacts, query) {
        val q = query.trim()
        val qd = q.filter { it.isDigit() || it == '+' }
        if (q.isEmpty()) contacts
        else contacts.filter { it.name.contains(q, ignoreCase = true) || (qd.isNotEmpty() && it.number.contains(qd)) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(12.dp))
        StaggerIn(0) {
            Column {
                Text("PHONEBOOK", color = style.subtle, fontSize = 12.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("Contacts", color = style.content, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))

        if (!granted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(84.dp).glass(CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Contacts, null, tint = style.subtle, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (denied) "Contacts permission denied.\nAllow it from the phone's app settings."
                        else "Allow access to your contacts\nto call saved numbers.",
                        color = style.subtle, fontSize = 15.sp, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    GlassKey(
                        modifier = Modifier.size(width = 200.dp, height = 52.dp),
                        shape = RoundedCornerShape(26.dp),
                        boost = 0.06f,
                        onClick = { launcher.launch(Manifest.permission.READ_CONTACTS) }
                    ) {
                        Text("ALLOW ACCESS", color = style.content, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                }
            }
        } else {
            GlassField(
                value = query,
                onValueChange = { query = it },
                label = "Search name or number",
                imeAction = ImeAction.Done,
                onDone = { focus.clearFocus() },
                leading = Icons.Filled.Search
            )
            Spacer(Modifier.height(12.dp))
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading…", color = style.subtle, fontSize = 15.sp)
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (contacts.isEmpty()) "No contacts found" else "No matches",
                        color = style.subtle, fontSize = 15.sp
                    )
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filtered, key = { i, it -> "${it.name}_${it.number}_$i" }) { _, item ->
                        ContactRow(item, onCall = { focus.clearFocus(); onCall(item.number) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(item: ContactItem, onCall: () -> Unit) {
    val style = LocalGlassStyle.current
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(22.dp), boost = 0.02f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCall() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).glass(CircleShape, boost = 0.06f), contentAlignment = Alignment.Center) {
            val initial = item.name.firstOrNull { it.isLetter() }?.uppercase()
            if (initial != null) {
                Text(initial, color = style.content, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            } else {
                Icon(Icons.Filled.Contacts, null, tint = style.content, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name, color = style.content, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (item.number != item.name) {
                Text(item.number, color = style.subtle, fontSize = 12.sp, maxLines = 1)
            }
        }
        Icon(Icons.Filled.Call, null, tint = GreenCall, modifier = Modifier.size(22.dp))
    }
}
