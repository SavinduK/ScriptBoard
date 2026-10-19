package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConnectedDeviceEntity
import com.example.sync.ClipboardSyncManager
import com.example.sync.QRCodeUtil
import com.example.sync.SyncActivityEvent
import com.example.ui.MainViewModel
import com.example.ui.keyboard.model.KeyboardColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedDevicesScreen(
    viewModel: MainViewModel,
    colors: KeyboardColors,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val syncManager = viewModel.syncManager

    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val syncLogs by viewModel.syncActivityLog.collectAsState()
    val syncEnabled by viewModel.clipboardSyncEnabled.collectAsState()
    val syncImages by viewModel.clipboardSyncImages.collectAsState()
    val localDeviceName by viewModel.localDeviceName.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }
    var showPairDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newDeviceNameInput by remember { mutableStateOf(localDeviceName) }

    val localIp = remember { syncManager.getLocalIpAddress() }
    val localPort = remember { syncManager.getActivePort() }
    val localDeviceId = remember { viewModel.keyboardPrefs.getLocalDeviceId() }

    val pairingJson = remember(localDeviceId, localDeviceName, localIp, localPort) {
        QRCodeUtil.buildPairingJson(
            deviceId = localDeviceId,
            deviceName = localDeviceName,
            ip = localIp,
            port = localPort,
            token = "keypro_sec_${localDeviceId.take(6)}"
        )
    }

    // QR image picker to decode QR screenshot
    val qrImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        val decoded = QRCodeUtil.decodeQrBitmap(bmp)
                        if (!decoded.isNullOrBlank()) {
                            val parsed = QRCodeUtil.parsePairingString(decoded)
                            if (parsed != null) {
                                viewModel.pairDevice(
                                    ip = parsed.ip,
                                    port = parsed.port,
                                    token = parsed.token,
                                    name = parsed.deviceName
                                ) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Successfully paired with ${parsed.deviceName}!", Toast.LENGTH_SHORT).show()
                                        showPairDialog = false
                                    } else {
                                        Toast.makeText(context, "Saved device (${parsed.deviceName}). Will auto-sync when online.", Toast.LENGTH_LONG).show()
                                        showPairDialog = false
                                    }
                                }
                            } else {
                                Toast.makeText(context, "QR code detected, but not a valid KeyPro pairing code", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Could not decode QR code from selected image", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to read image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.toolbarBackground)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_back_from_connected_devices")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.letterKeyTextColor
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Connected Devices",
                        color = colors.letterKeyTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Wi-Fi & Bluetooth Clipboard Continuity",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick Simulate Pair button for instant emulator testing
            FilledTonalButton(
                onClick = {
                    viewModel.simulateTestDevicePairing()
                    Toast.makeText(context, "Simulated Samsung Galaxy S24 Ultra paired!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .height(34.dp)
                    .testTag("btn_simulate_pair"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.enterKeyBackground.copy(alpha = 0.2f),
                    contentColor = colors.enterKeyBackground
                ),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Pair", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Local Device Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colors.enterKeyBackground.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = colors.enterKeyBackground,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = localDeviceName,
                                            color = colors.letterKeyTextColor,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = {
                                                newDeviceNameInput = localDeviceName
                                                showRenameDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename",
                                                tint = colors.letterKeySecondaryTextColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "IP: $localIp:$localPort • P2P Mesh Active",
                                        color = colors.letterKeySecondaryTextColor,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Active indicator badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Listening", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = colors.background.copy(alpha = 0.4f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Show QR / Scan QR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showQrDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_show_qr_code"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.enterKeyBackground,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Show QR", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showPairDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_scan_pair_device"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = colors.enterKeyBackground
                                )
                            ) {
                                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pair Device", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Sync Settings & Preferences Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sync Controls",
                            color = colors.letterKeyTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Global Sync Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Clipboard Sync", color = colors.letterKeyTextColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Sync copies across your phones and tablets", color = colors.letterKeySecondaryTextColor, fontSize = 11.sp)
                            }
                            Switch(
                                checked = syncEnabled,
                                onCheckedChange = { viewModel.setClipboardSync(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.enterKeyBackground
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = colors.background.copy(alpha = 0.4f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Images Sync Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sync Images as Well as Text", color = colors.letterKeyTextColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Transfer screenshots and copied photos over Wi-Fi", color = colors.letterKeySecondaryTextColor, fontSize = 11.sp)
                            }
                            Switch(
                                checked = syncImages,
                                onCheckedChange = { viewModel.setClipboardSyncImages(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.enterKeyBackground
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = colors.background.copy(alpha = 0.4f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Anti-Cyclic Sync Notice
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.background.copy(alpha = 0.5f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Anti-Cyclic Loop Protection: Unique sync IDs & origin tagging prevent echo loops automatically.",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Paired Devices Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paired Devices (${pairedDevices.size})",
                        color = colors.letterKeyTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (pairedDevices.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.sendTestBroadcast("Hello from ${viewModel.localDeviceName.value}! 📋")
                                Toast.makeText(context, "Test clip sent to all connected devices!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send Test Clip", fontSize = 12.sp, color = colors.enterKeyBackground)
                        }
                    }
                }
            }

            // Paired Devices List
            if (pairedDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = colors.letterKeySecondaryTextColor,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Devices Connected",
                                color = colors.letterKeyTextColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Open this screen on another device on the same Wi-Fi network and tap 'Pair Device' to scan the QR code.",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(pairedDevices, key = { it.deviceId }) { device ->
                    PairedDeviceItem(
                        device = device,
                        colors = colors,
                        onSendTest = {
                            viewModel.sendTestBroadcast("Test from ${viewModel.localDeviceName.value} to ${device.name}")
                            Toast.makeText(context, "Clip sent to ${device.name}", Toast.LENGTH_SHORT).show()
                        },
                        onUnpair = {
                            viewModel.unpairDevice(device.deviceId)
                            Toast.makeText(context, "Unpaired ${device.name}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Live Sync Activity Stream
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Live Sync Activity",
                    color = colors.letterKeyTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (syncLogs.isEmpty()) {
                item {
                    Text(
                        text = "No sync activity recorded yet. Copies will appear here live.",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(syncLogs.take(10), key = { it.id }) { log ->
                    SyncLogItem(log = log, colors = colors)
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // QR Code Display Dialog
    if (showQrDialog) {
        val qrBitmap = remember(pairingJson) {
            QRCodeUtil.generateQrBitmap(pairingJson, 512)
        }

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Text(
                    text = "Scan to Connect",
                    fontWeight = FontWeight.Bold,
                    color = colors.letterKeyTextColor
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan this QR code from your second phone or tablet on the same Wi-Fi network:",
                        fontSize = 12.sp,
                        color = colors.letterKeySecondaryTextColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // QR Code Graphic on clean white card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Pairing QR Code",
                            modifier = Modifier
                                .size(220.dp)
                                .padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "$localDeviceName • $localIp:$localPort",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.letterKeyTextColor
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Pairing JSON", pairingJson))
                            Toast.makeText(context, "Pairing details copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Pairing Code", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQrDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
                ) {
                    Text("Done")
                }
            },
            containerColor = colors.letterKeyBackground
        )
    }

    // Pair Device Dialog (Manual IP / Photo QR Picker)
    if (showPairDialog) {
        var ipInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf("") }
        var isConnecting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isConnecting) showPairDialog = false },
            title = {
                Text(
                    text = "Pair New Device",
                    fontWeight = FontWeight.Bold,
                    color = colors.letterKeyTextColor
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose how to pair with your other device:",
                        fontSize = 12.sp,
                        color = colors.letterKeySecondaryTextColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 1: Pick QR Image / Screenshot
                    Button(
                        onClick = {
                            qrImagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan QR from Photo / Screenshot", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = colors.background)
                        Text("  OR ENTER IP MANUALLY  ", fontSize = 10.sp, color = colors.letterKeySecondaryTextColor)
                        Divider(modifier = Modifier.weight(1f), color = colors.background)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Peer IP:Port (e.g. 192.168.1.150:8765)") },
                        placeholder = { Text("192.168.1.150") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.enterKeyBackground,
                            focusedLabelColor = colors.enterKeyBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Device Name (Optional)") },
                        placeholder = { Text("Galaxy Tab S9") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.enterKeyBackground,
                            focusedLabelColor = colors.enterKeyBackground
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = QRCodeUtil.parsePairingString(ipInput)
                        if (parsed != null) {
                            isConnecting = true
                            viewModel.pairDevice(
                                ip = parsed.ip,
                                port = parsed.port,
                                token = parsed.token,
                                name = nameInput.ifBlank { parsed.deviceName }
                            ) { success ->
                                isConnecting = false
                                if (success) {
                                    Toast.makeText(context, "Connected to ${parsed.deviceName}!", Toast.LENGTH_SHORT).show()
                                    showPairDialog = false
                                } else {
                                    Toast.makeText(context, "Saved device. Will sync once connected on Wi-Fi.", Toast.LENGTH_SHORT).show()
                                    showPairDialog = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter a valid IP address or pairing code", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
                ) {
                    Text(if (isConnecting) "Connecting..." else "Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPairDialog = false }) {
                    Text("Cancel", color = colors.letterKeySecondaryTextColor)
                }
            },
            containerColor = colors.letterKeyBackground
        )
    }

    // Rename Device Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(text = "Rename This Device", fontWeight = FontWeight.Bold, color = colors.letterKeyTextColor)
            },
            text = {
                OutlinedTextField(
                    value = newDeviceNameInput,
                    onValueChange = { newDeviceNameInput = it },
                    singleLine = true,
                    label = { Text("Device Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.enterKeyBackground,
                        focusedLabelColor = colors.enterKeyBackground
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setDeviceName(newDeviceNameInput)
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.enterKeyBackground)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = colors.letterKeySecondaryTextColor)
                }
            },
            containerColor = colors.letterKeyBackground
        )
    }
}

@Composable
private fun PairedDeviceItem(
    device: ConnectedDeviceEntity,
    colors: KeyboardColors,
    onSendTest: () -> Unit,
    onUnpair: () -> Unit
) {
    val lastSyncFormatted = remember(device.lastSyncedTimestamp) {
        val diffSec = (System.currentTimeMillis() - device.lastSyncedTimestamp) / 1000
        when {
            diffSec < 60 -> "Just now"
            diffSec < 3600 -> "${diffSec / 60}m ago"
            else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(device.lastSyncedTimestamp))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (device.isOnline) Color(0xFF4CAF50).copy(alpha = 0.15f)
                            else colors.background
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = if (device.isOnline) Color(0xFF4CAF50) else colors.letterKeySecondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = device.name,
                        color = colors.letterKeyTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${device.ipAddress}:${device.port} • Synced $lastSyncFormatted",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSendTest, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send test clip",
                        tint = colors.enterKeyBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onUnpair, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Unpair",
                        tint = colors.letterKeySecondaryTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncLogItem(log: SyncActivityEvent, colors: KeyboardColors) {
    val timeFormatted = remember(log.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }

    val icon = when {
        log.isLoopPrevented -> Icons.Default.Block
        log.isOutgoing -> Icons.Default.ArrowUpward
        log.isImage -> Icons.Default.Image
        else -> Icons.Default.ArrowDownward
    }

    val iconTint = when {
        log.isLoopPrevented -> Color(0xFFFFA000)
        log.isOutgoing -> colors.enterKeyBackground
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.title,
                    color = colors.letterKeyTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = log.description,
                    color = colors.letterKeySecondaryTextColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = timeFormatted,
                color = colors.letterKeySecondaryTextColor.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
        }
    }
}
