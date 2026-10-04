package com.cineai

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import android.graphics.BitmapFactory
import android.widget.VideoView
import android.view.ViewGroup
import android.net.Uri as AndroidUri
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Intent
import androidx.compose.ui.viewinterop.AndroidView
import com.cineai.data.MediaEntity
import com.cineai.data.MediaKind
import com.cineai.ui.CineTheme
import com.cineai.ui.MainViewModel
import java.io.File
import kotlinx.coroutines.launch

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{CineTheme{CineApp()}}}}

@Composable fun CineApp(vm:MainViewModel=viewModel()){
    var tab by remember{mutableIntStateOf(0)}; var picked by remember{mutableStateOf<MediaEntity?>(null)}; val media by vm.media.collectAsState()
    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){u->u?.let{vm.import(it){picked=it}}}
    val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){u->u?.let{vm.import(it){picked=it}}}
    Scaffold(containerColor=Color(0xFF090A0C),bottomBar={NavigationBar(containerColor=Color(0xFF101216)){listOf("Home","Library","Enhance","Settings").forEachIndexed{i,s->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(if(i==0)Icons.Default.Home else if(i==1)Icons.Default.Collections else if(i==2)Icons.Default.AutoAwesome else Icons.Default.Settings,s)},label={Text(s)})}}}){pad->Box(Modifier.padding(pad).fillMaxSize()){when(tab){0->Home(media, {photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}, {videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))}, {picked=it;tab=2});1->Library(media,{picked=it;tab=2});2->picked?.let{EnhanceScreen(it,vm)}?:EmptyEnhance();3->SettingsScreen()}}}
}

@Composable fun Header(title:String,sub:String?=null){Column(Modifier.fillMaxWidth().padding(20.dp)){Text("CINEAI",fontSize=14.sp,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold,letterSpacing=3.sp);Text(title,fontSize=30.sp,fontWeight=FontWeight.Bold);sub?.let{Text(it,color=Color.LightGray)}}}
@Composable fun Home(media:List<MediaEntity>,photo:()->Unit,video:()->Unit,open:(MediaEntity)->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Header("Make ordinary footage feel cinematic","Local computational photography. No cloud upload.");Row(Modifier.padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){Action("Import photo",Icons.Default.AddPhotoAlternate,photo,Modifier.weight(1f));Action("Import video",Icons.Default.VideoLibrary,video,Modifier.weight(1f))};Spacer(Modifier.height(20.dp));Card(Modifier.padding(20.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFF17191D))){Column(Modifier.padding(20.dp)){Text("AUTO AI",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text("Scene-aware enhancement",fontSize=23.sp,fontWeight=FontWeight.SemiBold);Text("Exposure • tonal curve • colour • detail • skin protection",color=Color.LightGray);Spacer(Modifier.height(14.dp));Text("Works offline",color=Color.White)}};Text("Recent projects",Modifier.padding(20.dp),fontWeight=FontWeight.Bold,fontSize=19.sp);media.take(4).forEach{MediaRow(it){open(it)}}}}
@Composable fun Action(text:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit,modifier:Modifier){Button(onClick=onClick,modifier=modifier.height(58.dp),shape=RoundedCornerShape(18.dp)){Icon(icon,null);Spacer(Modifier.width(8.dp));Text(text)}}
@Composable fun Library(media:List<MediaEntity>,open:(MediaEntity)->Unit){Column(Modifier.fillMaxSize()){Header("Library","Your originals stay private until you export.");LazyVerticalGrid(GridCells.Fixed(2),contentPadding=PaddingValues(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(media,key={it.id}){MediaCard(it){open(it)}}}}}
@Composable fun MediaCard(item:MediaEntity,onClick:()->Unit){Card(onClick=onClick,shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF15171B))){Column{if(item.kind==MediaKind.PHOTO.name){val bmp=BitmapFactory.decodeFile(item.originalPath);bmp?.let{androidx.compose.foundation.Image(it.asImageBitmap(),null,Modifier.fillMaxWidth().height(150.dp),contentScale=ContentScale.Crop)}}else Box(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF20242A)),contentAlignment=Alignment.Center){Icon(Icons.Default.PlayCircle,null,Modifier.size(48.dp),tint=MaterialTheme.colorScheme.primary)};Column(Modifier.padding(12.dp)){Text(item.displayName,maxLines=1);Text("${item.width}×${item.height}  •  ${item.status}",fontSize=11.sp,color=Color.Gray)}}}}
@Composable fun MediaRow(i:MediaEntity,open:()->Unit){ListItem(headlineContent={Text(i.displayName)},supportingContent={Text(i.status)},leadingContent={Icon(if(i.kind=="VIDEO")Icons.Default.VideoFile else Icons.Default.Image,null)},trailingContent={Text("OPEN",color=MaterialTheme.colorScheme.primary)},modifier=Modifier.clickable{open()})}
@Composable fun EnhanceScreen(item:MediaEntity,vm:MainViewModel){
    var before by remember{mutableStateOf(true)}
    var selectedPreset by remember{mutableStateOf(com.cineai.engine.Preset.AUTO)}
    var profile by remember{mutableStateOf(com.cineai.engine.EnhancementProfile())}
    var exportMessage by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
    val current=vm.media.collectAsState().value.firstOrNull{it.id==item.id} ?: item
    val work by vm.workFlow(current.id).collectAsState(initial=null)
    val progress=work?.progress?.getInt("progress",0) ?: 0
    val stage=work?.progress?.getString("stage") ?: if(current.status=="PROCESSING") "Processing" else "Ready"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
        Header("Enhance",current.displayName)
        Box(Modifier.padding(16.dp).fillMaxWidth().height(390.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF15171B))){
            if(current.kind=="PHOTO"){
                val path=if(!before && current.enhancedPath!=null)current.enhancedPath else current.originalPath
                BitmapFactory.decodeFile(path)?.let{androidx.compose.foundation.Image(it.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)}
            } else {
                val path=if(!before && current.enhancedPath!=null)current.enhancedPath else current.originalPath
                VideoPreview(path)
            }
            Text(if(before)"BEFORE" else "AFTER",Modifier.align(Alignment.TopStart).padding(14.dp),color=Color.White,fontWeight=FontWeight.Bold)
        }
        Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            FilterChip(before,{before=true},label={Text("Before")}); FilterChip(!before,{before=false},label={Text("After")})
        }
        Text("Presets",Modifier.padding(16.dp),fontWeight=FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            com.cineai.engine.Preset.values().forEach{p->AssistChip(onClick={selectedPreset=p;profile=com.cineai.engine.EnhancementProfile.fromPreset(p)},label={Text(p.label)})}
        }
        if(work?.state==androidx.work.WorkInfo.State.RUNNING || current.status=="PROCESSING"){Column(Modifier.padding(horizontal=16.dp)){Text(stage,color=MaterialTheme.colorScheme.primary);LinearProgressIndicator({progress/100f},Modifier.fillMaxWidth());Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("$progress%");TextButton(onClick={vm.cancel(current.id)}){Text("CANCEL")}}}}
        Button(onClick={vm.enhance(current.id,selectedPreset,if(current.kind=="PHOTO")3840 else 1080,if(selectedPreset==com.cineai.engine.Preset.AUTO)null else profile)},modifier=Modifier.padding(16.dp).fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp)){
            Icon(Icons.Default.AutoAwesome,null);Spacer(Modifier.width(8.dp));Text("AI ENHANCE")
        }
        Text("Manual editor",Modifier.padding(horizontal=16.dp),fontWeight=FontWeight.Bold)
        val controls=if(current.kind=="PHOTO") listOf(
            "Exposure" to profile.exposure,"Brightness" to profile.brightness,"Contrast" to profile.contrast,"Highlights" to profile.highlights,"Shadows" to profile.shadows,"Whites" to profile.whites,"Blacks" to profile.blacks,"Temperature" to profile.temperature,"Tint" to profile.tint,"Saturation" to profile.saturation,"Vibrance" to profile.vibrance,"Sharpness" to profile.sharpness,"Texture" to profile.texture,"Noise reduction" to profile.noiseReduction,"Vignette" to profile.vignette,"Grain" to profile.grain,"Cinematic intensity" to profile.cinematicIntensity) else listOf(
            "Exposure" to profile.exposure,"Brightness" to profile.brightness,"Contrast" to profile.contrast,"Highlights" to profile.highlights,"Shadows" to profile.shadows,"Temperature" to profile.temperature,"Tint" to profile.tint,"Saturation" to profile.saturation,"Vibrance" to profile.vibrance,"Sharpness" to profile.sharpness,"Noise reduction" to profile.noiseReduction)
        controls.forEachIndexed{i,(name,value)->SliderRow(name,value){v->val a=List(17){0f}.toMutableList();a[0]=profile.exposure;a[1]=profile.brightness;a[2]=profile.contrast;a[3]=profile.highlights;a[4]=profile.shadows;a[5]=profile.whites;a[6]=profile.blacks;a[7]=profile.temperature;a[8]=profile.tint;a[9]=profile.saturation;a[10]=profile.vibrance;a[11]=profile.sharpness;a[12]=profile.texture;a[13]=profile.noiseReduction;a[14]=profile.vignette;a[15]=profile.grain;a[16]=profile.cinematicIntensity;a[i]=v;profile=profile.copyFrom(a)}}
        Row(Modifier.padding(16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            OutlinedButton(onClick={profile=com.cineai.engine.EnhancementProfile.fromPreset(selectedPreset) },modifier=Modifier.weight(1f)){Text("RESET")}
            Button(onClick={vm.enhance(current.id,selectedPreset,if(current.kind=="PHOTO")3840 else 1080,profile)},modifier=Modifier.weight(1f)){Text("APPLY")}
        }
        Button(onClick={
            if(current.enhancedPath==null){exportMessage="Enhance the media before exporting."}
            else scope.launch { try { val uri=vm.export(current); context.startActivity(Intent(Intent.ACTION_VIEW).apply{setDataAndType(uri,current.mimeType);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)}); exportMessage="Export complete" } catch(t:Throwable){exportMessage=t.message?:"Export failed"} }
        },modifier=Modifier.padding(horizontal=16.dp).fillMaxWidth().height(56.dp)){Text("EXPORT")}
        exportMessage?.let{Text(it,Modifier.padding(16.dp),color=MaterialTheme.colorScheme.primary)}
        Spacer(Modifier.height(24.dp))
    }
}

@Composable fun SliderRow(name:String,value:Float,onValue:(Float)->Unit){Column(Modifier.padding(horizontal=16.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name);Text(String.format("%.2f",value),color=Color.Gray)};Slider(value,onValue,valueRange=-1f..1f)}}

@Composable fun VideoPreview(path:String){
    val context=LocalContext.current
    val player=remember(path){ExoPlayer.Builder(context).build().apply{setMediaItem(androidx.media3.common.MediaItem.fromUri(AndroidUri.fromFile(java.io.File(path))));prepare();playWhenReady=false}}
    DisposableEffect(player){onDispose{player.release()}}
    AndroidView(factory={ctx->PlayerView(ctx).apply{useController=true;this.player=player}},modifier=Modifier.fillMaxSize())
}

@Composable fun EmptyEnhance(){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Import media from Home to begin",color=Color.Gray)}}
@Composable fun SettingsScreen(){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Header("Settings","Privacy-first processing controls.");ListItem(headlineContent={Text("Processing")},supportingContent={Text("Photos default to 4K long-side output; videos default to 1080p when supported.")});ListItem(headlineContent={Text("Local processing")},supportingContent={Text("Imported originals and intermediate files stay in CINEAI private storage. Core enhancement does not require an API key or cloud upload.")});ListItem(headlineContent={Text("Gallery export")},supportingContent={Text("Only successful final outputs are written to Pictures/CINEAI or Movies/CINEAI using MediaStore.")});ListItem(headlineContent={Text("Physical-camera limitation")},supportingContent={Text("Software can improve tonal/color/detail response, but cannot recreate DSLR sensor, lens optics, depth of field, or genuine captured detail.")});Spacer(Modifier.height(20.dp));Text("Engine stack: Kotlin, Compose, Room, WorkManager, Media3 Transformer, Android Bitmap processing and bundled ML Kit face detection.",Modifier.padding(20.dp),color=Color.Gray)}}
