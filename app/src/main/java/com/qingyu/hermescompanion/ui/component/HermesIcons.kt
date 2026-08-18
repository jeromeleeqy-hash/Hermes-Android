package com.qingyu.hermescompanion.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.R

internal enum class HermesIconTone { NEUTRAL, PRIMARY, CYAN, VIOLET, SUCCESS, WARNING, ERROR }

/**
 * Semantic icon catalogue backed by the reviewed 256×256 masters in the design icon folders.
 *
 * Every UI call uses a semantic name rather than a raw resource so icon artwork and colour
 * remain consistent across navigation, chat, workspace, tasks and settings.
 */
enum class HermesIconKind(
    @param:DrawableRes val drawableRes: Int,
    internal val tone: HermesIconTone = HermesIconTone.NEUTRAL,
) {
    NAV_CHAT_OUTLINE(R.drawable.hermes_refined_nav_chat_outline),
    NAV_CHAT_FILLED(R.drawable.hermes_refined_nav_chat_filled, HermesIconTone.PRIMARY),
    NAV_SPACE_OUTLINE(R.drawable.hermes_refined_nav_space_outline),
    NAV_SPACE_FILLED(R.drawable.hermes_refined_nav_space_filled, HermesIconTone.PRIMARY),
    NAV_TASK_OUTLINE(R.drawable.hermes_refined_nav_tasks_outline),
    NAV_TASK_FILLED(R.drawable.hermes_refined_nav_tasks_filled, HermesIconTone.PRIMARY),
    NAV_PROFILE_OUTLINE(R.drawable.hermes_refined_nav_profile_outline),
    NAV_PROFILE_FILLED(R.drawable.hermes_refined_nav_profile_filled, HermesIconTone.PRIMARY),

    CHAT(R.drawable.hermes_refined_nav_chat_outline),
    SPACE(R.drawable.hermes_refined_nav_space_outline),
    TASK(R.drawable.hermes_refined_nav_tasks_outline),
    PROFILE(R.drawable.hermes_refined_nav_profile_outline),
    AI(R.drawable.hermes_refined_utility_ai_outline, HermesIconTone.VIOLET),
    PROJECT(R.drawable.hermes_refined_session_project_outline),
    NEW_CHAT(R.drawable.hermes_refined_conversation_compose_filled, HermesIconTone.PRIMARY),
    SEARCH(R.drawable.hermes_refined_common_search_outline, HermesIconTone.PRIMARY),
    RECENT(R.drawable.hermes_refined_session_recent_outline),
    ARCHIVE(R.drawable.hermes_refined_session_archive_outline, HermesIconTone.WARNING),
    PINNED(R.drawable.hermes_refined_session_pin_filled, HermesIconTone.WARNING),
    RENAME(R.drawable.hermes_refined_session_rename_outline),
    MOVE(R.drawable.hermes_refined_session_move_outline),
    DELETE(R.drawable.hermes_refined_session_delete_outline, HermesIconTone.ERROR),
    FOLDER(R.drawable.hermes_refined_workspace_folder_filled, HermesIconTone.WARNING),
    FOLDER_OPEN(R.drawable.hermes_refined_workspace_folder_open_filled, HermesIconTone.WARNING),
    FILE(R.drawable.hermes_refined_workspace_file_outline),
    MARKDOWN(R.drawable.hermes_refined_workspace_markdown_filled, HermesIconTone.SUCCESS),
    WEB(R.drawable.hermes_refined_workspace_web_outline, HermesIconTone.CYAN),
    ARTIFACT(R.drawable.hermes_refined_utility_artifact_outline, HermesIconTone.VIOLET),
    PHOTO(R.drawable.hermes_refined_workspace_image_filled, HermesIconTone.VIOLET),
    LINK(R.drawable.hermes_refined_utility_link_outline, HermesIconTone.PRIMARY),
    SOURCE_CHAT(R.drawable.hermes_refined_workspace_source_chat_outline, HermesIconTone.PRIMARY),
    PREVIEW(R.drawable.hermes_refined_workspace_preview_outline),
    DOCUMENT_EDIT(R.drawable.hermes_refined_workspace_edit_outline, HermesIconTone.PRIMARY),
    SAVE(R.drawable.hermes_refined_workspace_save_outline, HermesIconTone.PRIMARY),
    SHARE(R.drawable.hermes_refined_workspace_share_outline, HermesIconTone.PRIMARY),
    DOWNLOAD(R.drawable.hermes_refined_workspace_download_outline, HermesIconTone.PRIMARY),

    ATTACHMENT(R.drawable.hermes_refined_conversation_attach_outline),
    MICROPHONE(R.drawable.hermes_refined_conversation_microphone_outline, HermesIconTone.CYAN),
    SEND(R.drawable.hermes_refined_conversation_send_filled, HermesIconTone.PRIMARY),
    STOP(R.drawable.hermes_refined_task_stop_filled, HermesIconTone.ERROR),
    COMMAND(R.drawable.hermes_refined_conversation_command_outline),
    CLOSE(R.drawable.hermes_refined_common_close_outline),
    MORE(R.drawable.hermes_refined_conversation_more_outline),
    REFRESH(R.drawable.hermes_refined_task_refresh_outline, HermesIconTone.CYAN),
    ADD(R.drawable.hermes_refined_common_add_filled, HermesIconTone.PRIMARY),
    ADD_OUTLINE(R.drawable.hermes_refined_common_add_outline, HermesIconTone.PRIMARY),
    PLAY(R.drawable.hermes_refined_task_run_now_filled, HermesIconTone.SUCCESS),
    PAUSE(R.drawable.hermes_refined_task_pause_filled),
    EDIT(R.drawable.hermes_refined_profile_edit_profile_outline),
    CHECK(R.drawable.hermes_refined_common_check_outline, HermesIconTone.SUCCESS),
    CHECK_CIRCLE(R.drawable.hermes_refined_task_success_filled, HermesIconTone.SUCCESS),
    PENDING(R.drawable.hermes_refined_task_pending_outline, HermesIconTone.WARNING),
    UNCHECKED(R.drawable.hermes_refined_common_checkbox_empty_outline),
    ERROR(R.drawable.hermes_refined_task_failed_filled, HermesIconTone.ERROR),
    SYNC(R.drawable.hermes_refined_utility_sync_outline, HermesIconTone.CYAN),
    WAVEFORM(R.drawable.hermes_refined_utility_waveform_outline, HermesIconTone.CYAN),
    IDEA(R.drawable.hermes_refined_utility_idea_outline, HermesIconTone.WARNING),
    SUMMARIZE(R.drawable.hermes_refined_utility_summarize_outline),
    PLAN(R.drawable.hermes_refined_utility_plan_outline),

    APPEARANCE(R.drawable.hermes_refined_profile_appearance_outline, HermesIconTone.VIOLET),
    NOTIFICATION(R.drawable.hermes_refined_profile_notification_outline, HermesIconTone.ERROR),
    CONNECTION(R.drawable.hermes_refined_profile_gateway_outline, HermesIconTone.PRIMARY),
    INFORMATION(R.drawable.hermes_refined_profile_about_outline, HermesIconTone.PRIMARY),
    STORAGE(R.drawable.hermes_refined_workspace_download_outline, HermesIconTone.PRIMARY),
    VERIFIED(R.drawable.hermes_refined_profile_approval_filled, HermesIconTone.SUCCESS),
    MODEL(R.drawable.hermes_refined_profile_model_outline, HermesIconTone.CYAN),
    TODO(R.drawable.hermes_refined_utility_todo_outline),
    COUNCIL(R.drawable.hermes_refined_utility_council_outline, HermesIconTone.VIOLET),
    CAMERA_ADD(R.drawable.hermes_refined_profile_change_photo_outline, HermesIconTone.PRIMARY),
    SETTINGS(R.drawable.hermes_refined_profile_settings_outline),
    MEMORY(R.drawable.hermes_refined_profile_memory_file_outline, HermesIconTone.CYAN),
    SOUL(R.drawable.hermes_refined_profile_soul_file_outline, HermesIconTone.VIOLET),
    SKILLS(R.drawable.hermes_refined_profile_skills_outline, HermesIconTone.VIOLET),
    CONVERSATION_STYLE(R.drawable.hermes_refined_profile_chat_style_outline, HermesIconTone.CYAN),
    CHANGELOG(R.drawable.hermes_refined_profile_changelog_outline, HermesIconTone.CYAN),
    GUIDE(R.drawable.hermes_refined_profile_guide_outline, HermesIconTone.PRIMARY),

    BACK(R.drawable.hermes_refined_common_back_outline),
    CHEVRON_RIGHT(R.drawable.hermes_refined_common_next_outline),
    EXPAND_UP(R.drawable.hermes_refined_common_collapse_outline),
    EXPAND_DOWN(R.drawable.hermes_refined_common_expand_outline),
    OPEN_EXTERNAL(R.drawable.hermes_refined_common_open_external_outline, HermesIconTone.PRIMARY),
    EYE(R.drawable.hermes_refined_common_show_outline),
    EYE_OFF(R.drawable.hermes_refined_common_hide_outline),
    LOCK(R.drawable.hermes_refined_common_lock_outline),
    WARNING(R.drawable.hermes_refined_common_warning_filled, HermesIconTone.WARNING),
    LIGHT_MODE(R.drawable.hermes_refined_utility_light_mode_outline, HermesIconTone.WARNING),
    DARK_MODE(R.drawable.hermes_refined_utility_dark_mode_outline, HermesIconTone.VIOLET),
    SYSTEM_MODE(R.drawable.hermes_refined_utility_system_mode_outline, HermesIconTone.CYAN),
    COPY(R.drawable.hermes_refined_common_copy_outline),
    CHECKBOX_CHECKED(R.drawable.hermes_refined_common_checkbox_checked_filled, HermesIconTone.PRIMARY),
    CHECKBOX_EMPTY(R.drawable.hermes_refined_common_checkbox_empty_outline),
    BOLD(R.drawable.hermes_refined_utility_bold_outline),
    ITALIC(R.drawable.hermes_refined_utility_italic_outline),
    BULLET_LIST(R.drawable.hermes_refined_utility_bullet_list_outline),
    NUMBERED_LIST(R.drawable.hermes_refined_utility_numbered_list_outline),
    QUOTE(R.drawable.hermes_refined_utility_quote_outline),
    HORIZONTAL_RULE(R.drawable.hermes_refined_utility_horizontal_rule_outline),
    FOLDER_UP(R.drawable.hermes_refined_workspace_folder_open_outline, HermesIconTone.WARNING),
    HISTORY(R.drawable.hermes_refined_task_history_outline),
    LOADING(R.drawable.hermes_refined_utility_loading_outline),
    STATUS_CONNECTED(R.drawable.hermes_refined_utility_status_connected_filled, HermesIconTone.SUCCESS),
    STATUS_BUSY(R.drawable.hermes_refined_utility_status_busy_outline, HermesIconTone.WARNING),
    STATUS_ERROR(R.drawable.hermes_refined_utility_status_error_filled, HermesIconTone.ERROR),
    DRAG_HANDLE(R.drawable.hermes_refined_utility_drag_handle_outline),
    SWITCH_ON(R.drawable.hermes_refined_task_enable_filled, HermesIconTone.PRIMARY),
    SWITCH_OFF(R.drawable.hermes_refined_task_enable_outline),
    RADIO_SELECTED(R.drawable.hermes_refined_common_radio_selected_filled, HermesIconTone.PRIMARY),
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
) {
    val effectiveTint = tint ?: when (kind.tone) {
        HermesIconTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        HermesIconTone.PRIMARY -> MaterialTheme.colorScheme.primary
        HermesIconTone.CYAN -> Color(0xFF16A7B8)
        HermesIconTone.VIOLET -> Color(0xFF7666E8)
        HermesIconTone.SUCCESS -> Color(0xFF20B889)
        HermesIconTone.WARNING -> Color(0xFFE39A27)
        HermesIconTone.ERROR -> MaterialTheme.colorScheme.error
    }
    Image(
        painter = painterResource(kind.drawableRes),
        contentDescription = contentDescription,
        modifier = modifier.size(iconSize),
        colorFilter = ColorFilter.tint(effectiveTint),
    )
}

@Composable
fun HermesPinnedMarker(
    modifier: Modifier = Modifier,
    contentDescription: String? = "已置顶",
    iconSize: Dp = 16.dp,
) {
    HermesMulticolorIcon(
        kind = HermesIconKind.PINNED,
        contentDescription = contentDescription,
        modifier = modifier,
        iconSize = iconSize,
    )
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
