package com.qingyu.hermescompanion.ui

import com.qingyu.hermescompanion.model.UserProfilePreferences
import com.qingyu.hermescompanion.storage.AvatarTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarProfileTest {
    private val original = UserProfilePreferences(
        avatarUri = "file:/avatars/user.avatar",
        hermesAvatarUri = "file:/avatars/hermes.avatar",
    )

    @Test
    fun updatingUserAvatarDoesNotReplaceHermesAvatar() {
        val updated = updateAvatarUri(original, AvatarTarget.USER, "file:/avatars/user-new.avatar")

        assertEquals("file:/avatars/user-new.avatar", updated.avatarUri)
        assertEquals("file:/avatars/hermes.avatar", updated.hermesAvatarUri)
    }

    @Test
    fun updatingHermesAvatarDoesNotReplaceUserAvatar() {
        val updated = updateAvatarUri(original, AvatarTarget.HERMES, "file:/avatars/hermes-new.avatar")

        assertEquals("file:/avatars/user.avatar", updated.avatarUri)
        assertEquals("file:/avatars/hermes-new.avatar", updated.hermesAvatarUri)
    }
}
