package com.qingyu.hermescompanion.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

/** Subtle tactile feedback without a grey overlay on cards, tabs or buttons. */
object HermesPressIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node = PressNode(interactionSource)
    override fun equals(other: Any?) = other === this
    override fun hashCode() = javaClass.hashCode()

    private class PressNode(private val source: InteractionSource) : Modifier.Node(), DrawModifierNode {
        private val scale = Animatable(1f)
        override fun onAttach() {
            coroutineScope.launch {
                source.interactions.collect { interaction ->
                    val target = when (interaction) {
                        is PressInteraction.Press -> .985f
                        is PressInteraction.Release, is PressInteraction.Cancel -> 1f
                        else -> return@collect
                    }
                    coroutineScope.launch { scale.animateTo(target, tween(110)) }
                }
            }
        }
        override fun ContentDrawScope.draw() { scale(scale.value) { this@draw.drawContent() } }
    }
}
