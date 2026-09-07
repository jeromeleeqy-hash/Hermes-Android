package com.qingyu.hermescompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.ui.component.*

@Composable
internal fun WorkProgress(todos:List<ChatTodo>, tools:List<ToolActivity>) {
    val steps = if(todos.isNotEmpty()) todos.map { Triple(it.content,"",when(it.status){TodoStatus.COMPLETED->2;TodoStatus.IN_PROGRESS->1;else->0}) }
        else tools.takeLast(8).map { Triple(it.name,it.preview,when(it.status){ToolStatus.COMPLETED->2;ToolStatus.RUNNING->1;else->-1}) }
    if(steps.isEmpty()) return
    AssistantPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal=22.dp,vertical=20.dp)) {
            steps.forEachIndexed { i,(title,detail,status) ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Column(Modifier.width(28.dp).fillMaxHeight(),horizontalAlignment=Alignment.CenterHorizontally) {
                        val color=when(status){2->AssistantMint;1->AssistantBlue;-1->MaterialTheme.colorScheme.error;else->MaterialTheme.colorScheme.outline}
                        if(status==1) CircularProgressIndicator(Modifier.size(19.dp),color=AssistantBlue,strokeWidth=3.dp)
                        else AssistantIconWell(if(status==2)"check" else "run",color,Modifier.size(22.dp))
                        if(i!=steps.lastIndex) Box(Modifier.padding(top=7.dp,bottom=6.dp).width(1.dp).weight(1f).background(color.copy(alpha=.35f)))
                    }
                    Column(Modifier.weight(1f).padding(start=16.dp,bottom=if(i==steps.lastIndex)0.dp else 22.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(title,fontSize=16.sp,lineHeight=23.sp,fontWeight=FontWeight.Medium)
                        if(detail.isNotBlank()) Text(detail,fontSize=13.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                        if(status==-1) Text("本步未完成",fontSize=12.sp,color=MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
