package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConnectedDeviceEntity
import com.example.sync.SyncActivityEvent
import com.example.ui.MainViewModel
import com.example.ui.keyboard.model.KeyboardColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConnectedDevicesScreen(
    viewModel: MainViewModel,
    colors: KeyboardColors,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val syncLogs by viewModel.syncActivityLog.collectAsState()
    val syncEnabled by viewModel.clipboardSyncEnabled.collectAsState()
    val syncImages by viewModel.clipboardSyncImages.collectAsState()
    val localDeviceName by viewModel.localDeviceName.collectAsState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var newDeviceNameInput by remember { mutableStateOf(localDeviceName) }
    var isSigningIn by remember { mutableStateOf(false) }

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
                        text = "Cloud Clipboard Sync",
                        color = colors.letterKeyTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentUser != null) "Signed in as ${currentUser?.email ?: "Google Account"}" else "Google Account Sync",
                        color = colors.letterKeySecondaryTextColor,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick manual test broadcast
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val testText = "KeyPro Sync Test - ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
                        viewModel.sendTestBroadcast(testText)
                        Toast.makeText(context, "Test clip sent to Cloud", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_quick_test_sync")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Test Clip",
                        tint = colors.letterKeyTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // CARD 1: GOOGLE ACCOUNT & CLOUD STATUS
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_google_auth_status"),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.letterKeyBackground.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val user = currentUser
                        if (user != null) {
                            // Signed in state
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(colors.enterKeyBackground.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = colors.enterKeyBackground,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName?.ifBlank { "Google Account" } ?: "Google Account",
                                        color = colors.letterKeyTextColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = user.email ?: user.uid,
                                        color = colors.letterKeySecondaryTextColor,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isCloudConnected) Color(0xFF4CAF50) else Color(0xFFFFA000))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isCloudConnected) "Broadcasting to your devices" else "Connecting to Firebase...",
                                            color = if (isCloudConnected) Color(0xFF4CAF50) else Color(0xFFFFA000),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.signOut(context)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = colors.letterKeyTextColor
                                    ),
                                    modifier = Modifier.testTag("btn_google_sign_out")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sign Out", fontSize = 12.sp)
                                }
                            }
                        } else {
                            // Unauthenticated state
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Sync Across Your Devices",
                                    color = colors.letterKeyTextColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sign in with your Google account to automatically broadcast clipboard clips in real-time to all devices logged into this same Gmail.",
                                    color = colors.letterKeySecondaryTextColor,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        val activity = context as? Activity
                                        if (activity != null) {
                                            isSigningIn = true
                                            viewModel.signInWithGoogle(
                                                activity = activity,
                                                onSuccess = {
                                                    isSigningIn = false
                                                    Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    isSigningIn = false
                                                    Toast.makeText(context, "Sign in failed: $err", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.enterKeyBackground,
                                        contentColor = colors.enterKeyTextColor
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isSigningIn,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_google_sign_in")
                                ) {
                                    if (isSigningIn) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = colors.enterKeyTextColor,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Login,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sign in with Google",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CARD 2: SYNC PREFERENCES
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sync Controls",
                            color = colors.letterKeyTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Toggle 1: Enable Cloud Sync
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Share Clipboard to Cloud",
                                    color = colors.letterKeyTextColor,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Broadcast new copies to all signed-in devices instantly",
                                    color = colors.letterKeySecondaryTextColor,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = syncEnabled,
                                onCheckedChange = { viewModel.setClipboardSync(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.enterKeyTextColor,
                                    checkedTrackColor = colors.enterKeyBackground
                                ),
                                modifier = Modifier.testTag("switch_auto_share_cloud")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Toggle 2: Sync Images
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sync Copied Images",
                                    color = colors.letterKeyTextColor,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Share clipboard photos and screenshots across devices",
                                    color = colors.letterKeySecondaryTextColor,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = syncImages,
                                onCheckedChange = { viewModel.setClipboardSyncImages(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.enterKeyTextColor,
                                    checkedTrackColor = colors.enterKeyBackground
                                ),
                                modifier = Modifier.testTag("switch_sync_images")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Device Name Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.background.copy(alpha = 0.5f))
                                .clickable {
                                    newDeviceNameInput = localDeviceName
                                    showRenameDialog = true
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "This Device Name",
                                        color = colors.letterKeySecondaryTextColor,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = localDeviceName,
                                        color = colors.letterKeyTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Name",
                                tint = colors.letterKeySecondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // CARD 3: DEVICES ON THIS GOOGLE ACCOUNT
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connected Devices (${pairedDevices.size})",
                                    color = colors.letterKeyTextColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            TextButton(
                                onClick = {
                                    viewModel.simulateTestDevicePairing()
                                    Toast.makeText(context, "Added simulated tablet device", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("btn_simulate_tablet")
                            ) {
                                Text("Add Test Peer", fontSize = 11.sp, color = colors.enterKeyBackground)
                            }
                        }

                        if (pairedDevices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = null,
                                        tint = colors.letterKeySecondaryTextColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No other devices linked yet",
                                        color = colors.letterKeyTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Open KeyPro on your second phone or tablet and sign in with ${currentUser?.email ?: "the same Google account"}.",
                                        color = colors.letterKeySecondaryTextColor,
                                        fontSize = 11.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pairedDevices.forEach { device ->
                                    DeviceRowItem(
                                        device = device,
                                        colors = colors,
                                        onRemove = { viewModel.removeConnectedDevice(device.deviceId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // CARD 4: REALTIME CLOUD ACTIVITY LOG
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.letterKeyBackground.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = colors.enterKeyBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cloud Sync Activity",
                                    color = colors.letterKeyTextColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${syncLogs.size} events",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 11.sp
                            )
                        }

                        if (syncLogs.isEmpty()) {
                            Text(
                                text = "No cloud sync events recorded yet. Copy any text or image to sync!",
                                color = colors.letterKeySecondaryTextColor,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                syncLogs.take(15).forEach { event ->
                                    SyncEventRow(event = event, colors = colors)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Local Device Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text("Change Device Name", color = colors.letterKeyTextColor)
            },
            text = {
                OutlinedTextField(
                    value = newDeviceNameInput,
                    onValueChange = { newDeviceNameInput = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.letterKeyTextColor,
                        unfocusedTextColor = colors.letterKeyTextColor,
                        focusedBorderColor = colors.enterKeyBackground,
                        unfocusedBorderColor = colors.letterKeySecondaryTextColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_rename_device")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDeviceNameInput.isNotBlank()) {
                            viewModel.setDeviceName(newDeviceNameInput)
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.enterKeyBackground,
                        contentColor = colors.enterKeyTextColor
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = colors.letterKeySecondaryTextColor)
                }
            },
            containerColor = colors.popupBackground
        )
    }
}

@Composable
private fun DeviceRowItem(
    device: ConnectedDeviceEntity,
    colors: KeyboardColors,
    onRemove: () -> Unit
) {
    val isTablet = device.name.contains("Tab", ignoreCase = true) || device.name.contains("Tablet", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.background.copy(alpha = 0.6f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.enterKeyBackground.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isTablet) Icons.Default.TabletAndroid else Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = colors.enterKeyBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = device.name,
                    color = colors.letterKeyTextColor,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Synced via Firebase",
                        color = Color(0xFF4CAF50),
                        fontSize = 11.sp
                    )
                }
            }
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove Device",
                tint = colors.letterKeySecondaryTextColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SyncEventRow(
    event: SyncActivityEvent,
    colors: KeyboardColors
) {
    val timeStr = remember(event.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.background.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (event.isOutgoing) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = if (event.isOutgoing) colors.enterKeyBackground else Color(0xFF4CAF50),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                color = colors.letterKeyTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = event.description,
                color = colors.letterKeySecondaryTextColor,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = timeStr,
            color = colors.letterKeySecondaryTextColor.copy(alpha = 0.7f),
            fontSize = 9.5.sp
        )
    }
}
