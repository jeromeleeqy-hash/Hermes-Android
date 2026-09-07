package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.ui.AppUiState
import com.qingyu.hermescompanion.ui.component.*
import java.time.LocalTime

@Composable
fun AssistantHomeScreen(
    state: AppUiState, contentPadding: PaddingValues,
    onStart: (String) -> Unit, onOpen: (HermesSession) -> Unit,
    onHistory: () -> Unit, onTasks: () -> Unit, onFiles: () -> Unit,
    onRefresh: () -> Unit, onRespond: (AgentRequest, String) -> Unit,
    onSearch: () -> Unit, onAdd: (String) -> Unit, onVoice: (String) -> Unit,
    keyboardInsets: WindowInsets = WindowInsets.ime,
) {
    var draft by rememberSaveable(state.activeProfile, state.selectedProjectId) { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val name = state.userProfile.hermesDisplayName.ifBlank { "Hermes" }
    val user = state.userProfile.displayName.ifBlank { state.username }.takeIf { it.isNotBlank() }
    val runs = state.runningRuns.filter { it.session.profile == state.activeProfile }
    val sessions = state.sessions.filter { it.profile == state.activeProfile }
    val featured = sessions.firstOrNull { session -> runs.none { it.session.scopedId == session.scopedId } }
    val recent = (runs.map { it.session } + sessions.filterNot { state.pendingAgentRequests.isEmpty() && it.scopedId == featured?.scopedId }).distinctBy { it.scopedId }.take(2)
    val greeting = when (LocalTime.now().hour) { in 5..10 -> "早上好"; in 11..13 -> "中午好"; in 14..18 -> "下午好"; else -> "晚上好" }
    val enabled = !state.isBusy && !state.isProfileSwitching
    // Scaffold padding must be consumed before IME padding; otherwise the dock's
    // reserved height is counted again above the keyboard.
    Column(Modifier.fillMaxSize().testTag("home_root").padding(contentPadding)
        .consumeWindowInsets(contentPadding).windowInsetsPadding(keyboardInsets)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth().statusBarsPadding(), contentPadding=PaddingValues(start=14.dp,end=14.dp,top=8.dp,bottom=8.dp)) {
            item {
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box {
                        Row(Modifier.testTag("home_menu_anchor").clickable { menu=true },verticalAlignment=Alignment.CenterVertically) {
                            UserAvatar(state.userProfile.hermesAvatarUri,name,30.dp,hermesFallback=true,shape=CircleShape)
                            Text(name,Modifier.padding(start=10.dp),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
                        }
                        DropdownMenu(expanded=menu,onDismissRequest={menu=false},offset=DpOffset(0.dp,12.dp),modifier=Modifier.testTag("home_menu")) {
                            DropdownMenuItem(text={Text("项目与档案")},onClick={menu=false;onHistory()})
                            DropdownMenuItem(text={Text("执行中心")},onClick={menu=false;onTasks()})
                            DropdownMenuItem(text={Text("文件与成果")},onClick={menu=false;onFiles()})
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface) {
                        IconButton(onClick=onSearch,modifier=Modifier.size(40.dp).semantics { contentDescription="搜索对话" }) { AssistantGlyph("search",Modifier.size(23.dp)) }
                    }
                }
            }
            item {
                BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min=110.dp)) {
                    val girlWidth = maxWidth * .39f
                    ReferenceHermesGirl(Modifier.align(Alignment.BottomEnd).width(girlWidth).height(girlWidth * (267f/312f)))
                    Column(Modifier.fillMaxWidth(.62f).padding(start=8.dp,top=25.dp,bottom=22.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(if(user==null) "$greeting。" else "$greeting，$user",fontSize=23.sp,lineHeight=30.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Text(when { state.pendingAgentRequests.isNotEmpty() -> "今天，有 ${state.pendingAgentRequests.size} 件事需要你决定。"; runs.isNotEmpty() -> "有 ${runs.size} 件事正在进行，我在这里。"; else -> "今天，有什么事想交给我？" },fontSize=14.sp,lineHeight=21.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                if (state.pendingAgentRequests.isNotEmpty()) DecisionCard(state.pendingAgentRequests.first(),onRespond)
                else AssistantPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        HomeCardLabel("最近在聊","bulb",Color(0xFFC88E14))
                        Text(featured?.title?.ifBlank{"继续上次的想法"} ?: "从一件小事开始",fontSize=17.sp,lineHeight=24.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(featured?.preview?.takeIf{it.isNotBlank()} ?: "整理资料、记下想法，或聊聊今天的事。",fontSize=14.sp,lineHeight=21.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            HomeChoice(if(featured!=null) "继续这件事" else "聊聊新想法","history",AssistantMint,Modifier.weight(1f)) { if(featured!=null) onOpen(featured) else onStart(draft) }
                            HomeChoice("开个新话题","plus",AssistantPurple,Modifier.weight(1f)) { onStart(draft) }
                        }
                        TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer), onClick={if(featured!=null) onOpen(featured) else onStart(draft)},contentPadding=PaddingValues(0.dp)) {
                            Text(if(featured!=null) "接着聊聊" else "开始对话",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=AssistantBlue)
                            Spacer(Modifier.width(8.dp));AssistantGlyph("arrow",Modifier.size(21.dp),AssistantBlue)
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                AssistantPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            HomeCardLabel("接着上次的事","history",AssistantBlue,Modifier.weight(1f))
                            Text("全部",Modifier.clickable(onClick=onHistory).padding(horizontal=4.dp,vertical=8.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if(recent.isEmpty()) {
                            HomeRecentRow("任务与定时","查看任务、提醒和执行记录","history",AssistantBlue,false,onTasks)
                            HorizontalDivider(Modifier.padding(start=58.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.6f))
                            HomeRecentRow("文件与成果","整理过的资料，都在这里","file",AssistantPurple,false,onFiles)
                        } else recent.forEachIndexed { index,session ->
                            val run=runs.firstOrNull { it.session.scopedId==session.scopedId }
                            HomeRecentRow(session.title,run?.let { if(it.recovering) "正在恢复连接" else it.stage } ?: session.preview,
                                if(run!=null) "run" else "file",if(run!=null) AssistantBlue else AssistantPurple,
                                session.scopedId in state.unreadSessionIds) { onOpen(session) }
                            if(index!=recent.lastIndex) HorizontalDivider(Modifier.padding(start=58.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.6f))
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().clickable(onClick=onTasks).padding(horizontal=8.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically) {
                    AssistantIconWell("check",AssistantMint,Modifier.size(30.dp))
                    Text(when {state.pendingAgentRequests.isNotEmpty()->"${state.pendingAgentRequests.size} 项等待确认";runs.isNotEmpty()->"${runs.size} 件事正在处理";else->"暂时没有待确认事项"},Modifier.weight(1f).padding(start=10.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick=onFiles,modifier=Modifier.size(36.dp)) {AssistantGlyph("file",Modifier.size(18.dp))}
                }
            }
        }
        FixedRegionDivider()
        AssistantPanel(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=4.dp).testTag("home_composer")) {
            Row(Modifier.fillMaxWidth().heightIn(min=54.dp).padding(horizontal=6.dp,vertical=3.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={onAdd(draft)},enabled=enabled,modifier=Modifier.testTag("home_add").semantics { contentDescription="添加内容" }) {AssistantIconWell("plus",MaterialTheme.colorScheme.onSurfaceVariant,Modifier.size(30.dp))}
                BasicTextField(value=draft,onValueChange={draft=it},enabled=enabled,maxLines=4,
                    textStyle=MaterialTheme.typography.bodyLarge.copy(color=MaterialTheme.colorScheme.onSurface),cursorBrush=SolidColor(AssistantBlue),
                    keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send),keyboardActions=KeyboardActions(onSend={if(enabled) onStart(draft)}),
                    modifier=Modifier.weight(1f).padding(horizontal=3.dp,vertical=8.dp).testTag("home_input"),
                    decorationBox={inner->Box {if(draft.isEmpty()) Text("有什么事，交给我…",fontSize=16.sp,color=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.75f));inner()}})
                IconButton(onClick={if(draft.isBlank()) onVoice(draft) else onStart(draft)},enabled=enabled,modifier=Modifier.testTag("home_voice_send").semantics { contentDescription=if(draft.isBlank()) "语音输入" else "进入对话" }) {AssistantGlyph(if(draft.isBlank())"wave" else "arrow",Modifier.size(24.dp),AssistantBlue)}
            }
        }
    }
}

@Composable internal fun HomeCardLabel(text: String,icon: String,color: Color,modifier: Modifier=Modifier) {
    Row(modifier,verticalAlignment=Alignment.CenterVertically) {AssistantIconWell(icon,color,Modifier.size(32.dp));Text(text,Modifier.padding(start=11.dp),fontSize=14.sp,color=MaterialTheme.colorScheme.onSurface)}
}
@Composable internal fun HomeChoice(text: String,icon: String,color: Color,modifier: Modifier=Modifier,onClick:()->Unit) {
    Surface(modifier.clickable(onClick=onClick),shape=RoundedCornerShape(30.dp),color=color.copy(alpha=.09f)) {
        Row(Modifier.heightIn(min=46.dp).padding(horizontal=13.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            AssistantGlyph(icon,Modifier.size(20.dp),color);Text(text,Modifier.weight(1f),fontSize=14.sp,lineHeight=20.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
        }
    }
}
@Composable private fun HomeRecentRow(title:String,detail:String,icon:String,color:Color,unread:Boolean,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
        AssistantIconWell(icon,color,Modifier.size(44.dp));Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,fontSize=15.sp,lineHeight=22.sp,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(detail.ifBlank{"点击继续这段对话"},fontSize=13.sp,lineHeight=19.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        if(unread) Surface(Modifier.padding(horizontal=6.dp).size(6.dp),shape=CircleShape,color=AssistantMint) {}
        AssistantGlyph("chevron",Modifier.size(18.dp))
    }
}
