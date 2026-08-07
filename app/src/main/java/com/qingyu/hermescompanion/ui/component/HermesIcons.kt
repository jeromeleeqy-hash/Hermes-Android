package com.qingyu.hermescompanion.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.R

/** Hermes Light semantic icon set: 24 x 24 grid, light geometry and restrained color. */
enum class HermesIconKind(@param:DrawableRes val drawableRes: Int, @param:DrawableRes val nightDrawableRes: Int) {
    CHAT(R.drawable.hermes_bitmap_chat, R.drawable.hermes_bitmap_chat_night),
    SPACE(R.drawable.hermes_bitmap_space, R.drawable.hermes_bitmap_space_night),
    TASK(R.drawable.hermes_bitmap_task, R.drawable.hermes_bitmap_task_night),
    PROFILE(R.drawable.hermes_bitmap_profile, R.drawable.hermes_bitmap_profile_night),
    AI(R.drawable.hermes_bitmap_ai, R.drawable.hermes_bitmap_ai_night),
    PROJECT(R.drawable.hermes_bitmap_project, R.drawable.hermes_bitmap_project_night),
    NEW_CHAT(R.drawable.hermes_bitmap_new_chat, R.drawable.hermes_bitmap_new_chat_night),
    SEARCH(R.drawable.hermes_bitmap_search, R.drawable.hermes_bitmap_search_night),
    RECENT(R.drawable.hermes_bitmap_recent, R.drawable.hermes_bitmap_recent_night),
    ARCHIVE(R.drawable.hermes_bitmap_archive, R.drawable.hermes_bitmap_archive_night),
    PINNED(R.drawable.hermes_bitmap_pinned, R.drawable.hermes_bitmap_pinned_night),
    RENAME(R.drawable.hermes_bitmap_rename, R.drawable.hermes_bitmap_rename_night),
    MOVE(R.drawable.hermes_bitmap_move, R.drawable.hermes_bitmap_move_night),
    DELETE(R.drawable.hermes_bitmap_delete, R.drawable.hermes_bitmap_delete_night),
    FOLDER(R.drawable.hermes_bitmap_folder, R.drawable.hermes_bitmap_folder_night),
    FILE(R.drawable.hermes_bitmap_file, R.drawable.hermes_bitmap_file_night),
    ARTIFACT(R.drawable.hermes_bitmap_markdown, R.drawable.hermes_bitmap_markdown_night),
    PHOTO(R.drawable.hermes_bitmap_photo, R.drawable.hermes_bitmap_photo_night),
    LINK(R.drawable.hermes_bitmap_link, R.drawable.hermes_bitmap_link_night),
    ATTACHMENT(R.drawable.hermes_bitmap_attachment, R.drawable.hermes_bitmap_attachment_night),
    MICROPHONE(R.drawable.hermes_bitmap_microphone, R.drawable.hermes_bitmap_microphone_night),
    SEND(R.drawable.hermes_bitmap_send, R.drawable.hermes_bitmap_send_night),
    STOP(R.drawable.hermes_bitmap_stop, R.drawable.hermes_bitmap_stop_night),
    CLOSE(R.drawable.hermes_bitmap_close, R.drawable.hermes_bitmap_close_night),
    MORE(R.drawable.hermes_bitmap_more, R.drawable.hermes_bitmap_more_night),
    REFRESH(R.drawable.hermes_bitmap_refresh, R.drawable.hermes_bitmap_refresh_night),
    ADD(R.drawable.hermes_bitmap_add, R.drawable.hermes_bitmap_add_night),
    PLAY(R.drawable.hermes_bitmap_play, R.drawable.hermes_bitmap_play_night),
    PAUSE(R.drawable.hermes_bitmap_pause, R.drawable.hermes_bitmap_pause_night),
    EDIT(R.drawable.hermes_bitmap_edit, R.drawable.hermes_bitmap_edit_night),
    CHECK(R.drawable.hermes_bitmap_check, R.drawable.hermes_bitmap_check_night),
    CHECK_CIRCLE(R.drawable.hermes_bitmap_check_circle, R.drawable.hermes_bitmap_check_circle_night),
    PENDING(R.drawable.hermes_bitmap_pending, R.drawable.hermes_bitmap_pending_night),
    UNCHECKED(R.drawable.hermes_bitmap_unchecked, R.drawable.hermes_bitmap_unchecked_night),
    ERROR(R.drawable.hermes_bitmap_error, R.drawable.hermes_bitmap_error_night),
    SYNC(R.drawable.hermes_bitmap_sync, R.drawable.hermes_bitmap_sync_night),
    WAVEFORM(R.drawable.hermes_bitmap_waveform, R.drawable.hermes_bitmap_waveform_night),
    IDEA(R.drawable.hermes_bitmap_idea, R.drawable.hermes_bitmap_idea_night),
    SUMMARIZE(R.drawable.hermes_bitmap_summarize, R.drawable.hermes_bitmap_summarize_night),
    PLAN(R.drawable.hermes_bitmap_plan, R.drawable.hermes_bitmap_plan_night),
    APPEARANCE(R.drawable.hermes_bitmap_appearance, R.drawable.hermes_bitmap_appearance_night),
    NOTIFICATION(R.drawable.hermes_bitmap_notification, R.drawable.hermes_bitmap_notification_night),
    CONNECTION(R.drawable.hermes_bitmap_connection, R.drawable.hermes_bitmap_connection_night),
    INFORMATION(R.drawable.hermes_bitmap_information, R.drawable.hermes_bitmap_information_night),
    STORAGE(R.drawable.hermes_bitmap_storage, R.drawable.hermes_bitmap_storage_night),
    VERIFIED(R.drawable.hermes_bitmap_verified, R.drawable.hermes_bitmap_verified_night),
    MODEL(R.drawable.hermes_bitmap_model, R.drawable.hermes_bitmap_model_night),
    TODO(R.drawable.hermes_bitmap_todo, R.drawable.hermes_bitmap_todo_night),
    BACK(R.drawable.hermes_bitmap_back, R.drawable.hermes_bitmap_back_night),
    CHEVRON_RIGHT(R.drawable.hermes_bitmap_chevron_right, R.drawable.hermes_bitmap_chevron_right_night),
    EXPAND_UP(R.drawable.hermes_bitmap_expand_up, R.drawable.hermes_bitmap_expand_up_night),
    EXPAND_DOWN(R.drawable.hermes_bitmap_expand_down, R.drawable.hermes_bitmap_expand_down_night),
    OPEN_EXTERNAL(R.drawable.hermes_bitmap_open_external, R.drawable.hermes_bitmap_open_external_night),
    EYE(R.drawable.hermes_bitmap_eye, R.drawable.hermes_bitmap_eye_night),
    EYE_OFF(R.drawable.hermes_bitmap_eye_off, R.drawable.hermes_bitmap_eye_off_night),
    LOCK(R.drawable.hermes_bitmap_lock, R.drawable.hermes_bitmap_lock_night),
    WARNING(R.drawable.hermes_bitmap_warning, R.drawable.hermes_bitmap_warning_night),
    LIGHT_MODE(R.drawable.hermes_bitmap_light_mode, R.drawable.hermes_bitmap_light_mode_night),
    DARK_MODE(R.drawable.hermes_bitmap_dark_mode, R.drawable.hermes_bitmap_dark_mode_night),
    SYSTEM_MODE(R.drawable.hermes_bitmap_system_mode, R.drawable.hermes_bitmap_system_mode_night),
    COPY(R.drawable.hermes_bitmap_copy, R.drawable.hermes_bitmap_copy_night),
    CHECKBOX_CHECKED(R.drawable.hermes_bitmap_checkbox_checked, R.drawable.hermes_bitmap_checkbox_checked_night),
    CHECKBOX_EMPTY(R.drawable.hermes_bitmap_checkbox_empty, R.drawable.hermes_bitmap_checkbox_empty_night),
    BOLD(R.drawable.hermes_bitmap_bold, R.drawable.hermes_bitmap_bold_night),
    ITALIC(R.drawable.hermes_bitmap_italic, R.drawable.hermes_bitmap_italic_night),
    BULLET_LIST(R.drawable.hermes_bitmap_bullet_list, R.drawable.hermes_bitmap_bullet_list_night),
    NUMBERED_LIST(R.drawable.hermes_bitmap_numbered_list, R.drawable.hermes_bitmap_numbered_list_night),
    QUOTE(R.drawable.hermes_bitmap_quote, R.drawable.hermes_bitmap_quote_night),
    HORIZONTAL_RULE(R.drawable.hermes_bitmap_horizontal_rule, R.drawable.hermes_bitmap_horizontal_rule_night),
    FOLDER_UP(R.drawable.hermes_bitmap_folder_up, R.drawable.hermes_bitmap_folder_up_night),
    HISTORY(R.drawable.hermes_bitmap_history, R.drawable.hermes_bitmap_history_night),
    LOADING(R.drawable.hermes_bitmap_loading, R.drawable.hermes_bitmap_loading_night),
    STATUS_CONNECTED(R.drawable.hermes_bitmap_status_connected, R.drawable.hermes_bitmap_status_connected_night),
    STATUS_BUSY(R.drawable.hermes_bitmap_status_busy, R.drawable.hermes_bitmap_status_busy_night),
    STATUS_ERROR(R.drawable.hermes_bitmap_status_error, R.drawable.hermes_bitmap_status_error_night),
    DRAG_HANDLE(R.drawable.hermes_bitmap_drag_handle, R.drawable.hermes_bitmap_drag_handle_night),
    SWITCH_ON(R.drawable.hermes_bitmap_switch_on, R.drawable.hermes_bitmap_switch_on_night),
    SWITCH_OFF(R.drawable.hermes_bitmap_switch_off, R.drawable.hermes_bitmap_switch_off_night),
    RADIO_SELECTED(R.drawable.hermes_bitmap_radio_selected, R.drawable.hermes_bitmap_radio_selected_night),
}

enum class HermesStatusKind(internal val icon: HermesIconKind) {
    CONNECTED(HermesIconKind.STATUS_CONNECTED),
    BUSY(HermesIconKind.STATUS_BUSY),
    ERROR(HermesIconKind.STATUS_ERROR),
}

@Composable
fun HermesMulticolorIcon(
    kind: HermesIconKind,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    tint: Color? = null,
    grayscale: Boolean = false,
) {
    val effectiveTint = tint ?: if (kind == HermesIconKind.BACK) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        null
    }
    Image(
        painter = hermesIconPainter(kind),
        contentDescription = contentDescription,
        modifier = modifier.size(iconSize),
        colorFilter = when {
            effectiveTint != null -> ColorFilter.tint(effectiveTint)
            grayscale -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            else -> null
        },
    )
}

@Composable
fun HermesPinnedMarker(
    modifier: Modifier = Modifier,
    contentDescription: String? = "已置顶",
) {
    Image(
        painter = hermesIconPainter(HermesIconKind.PINNED),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

@Composable
private fun hermesIconPainter(kind: HermesIconKind): androidx.compose.ui.graphics.painter.Painter {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return painterResource(if (dark) kind.nightDrawableRes else kind.drawableRes)
}

@Composable
fun HermesStatusIcon(
    status: HermesStatusKind,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    HermesMulticolorIcon(
        kind = status.icon,
        contentDescription = contentDescription,
        modifier = modifier,
        iconSize = 12.dp,
    )
}
