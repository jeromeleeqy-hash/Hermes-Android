package com.qingyu.hermescompanion.ui.screen

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import com.qingyu.hermescompanion.model.ConnectionConfig
import com.qingyu.hermescompanion.model.UserProfilePreferences
import com.qingyu.hermescompanion.storage.SecureConfigStore
import com.qingyu.hermescompanion.ui.HermesViewModel
import com.qingyu.hermescompanion.ui.component.ScreenAwakeLease
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class Onboarding363Test {
    private val app get()=RuntimeEnvironment.getApplication()
    private val models=ViewModelStore()
    private fun model(key:String)=HermesViewModel(app).also { models.put(key,it) }
    @Before fun reset() { app.getSharedPreferences("hermes_secure_connection",Context.MODE_PRIVATE).edit().clear().commit() }
    @After fun finish() { models.clear() }
    @Test fun freshInstallCompletesWelcomeThenLocalIdentityAndPersistsBoth() {
        val first=model("first")
        assertTrue(first.uiState.showLaunchIntro);assertTrue(first.uiState.needsIdentitySetup)
        first.finishLaunchIntro()
        assertFalse(first.uiState.showLaunchIntro);assertTrue(first.uiState.needsIdentitySetup)
        first.completeLocalIdentity(UserProfilePreferences(hermesDisplayName="月",displayName="Alex"))
        assertFalse(first.uiState.needsIdentitySetup);assertFalse(first.uiState.hasSavedConnection)
        val second=model("second")
        assertFalse(second.uiState.showLaunchIntro);assertFalse(second.uiState.needsIdentitySetup)
        assertEquals("月",second.uiState.userProfile.hermesDisplayName)
        second.replayLaunchIntro();assertTrue(second.uiState.showLaunchIntro)
        second.finishLaunchIntro();assertFalse(second.uiState.needsIdentitySetup)
    }
    @Test fun upgradingAndSigningOutPreservesCustomIdentityAndDoesNotRenameGatewayAccount() {
        val store=SecureConfigStore(app)
        store.save(ConnectionConfig("https://gateway.example","agent-user"))
        store.saveUserProfile(UserProfilePreferences(hermesDisplayName="Luna",displayName="Alex",bio="Local profile"))
        val vm=model("upgrade")
        assertTrue(vm.uiState.hasSavedConnection);assertFalse(vm.uiState.needsIdentitySetup)
        assertEquals("Luna",vm.uiState.userProfile.hermesDisplayName)
        vm.finishLaunchIntro()
        vm.updateUserProfile(vm.uiState.userProfile.copy(hermesDisplayName="Mira"))
        assertEquals("agent-user",store.read()?.username)
        vm.disconnect()
        assertEquals("Mira",vm.uiState.userProfile.hermesDisplayName)
        assertEquals("Alex",vm.uiState.userProfile.displayName)
        assertNull(store.read())
        assertFalse(model("restart").uiState.needsIdentitySetup)
    }
    @Test fun voiceScreenAwakeFollowsForegroundAndRestoresOriginalFlag() {
        val owner=object:LifecycleOwner {
            val registry=LifecycleRegistry.createUnsafe(this)
            override val lifecycle:Lifecycle get()=registry
        }
        val view=View(app)
        owner.registry.currentState=Lifecycle.State.STARTED
        val lease=ScreenAwakeLease(view,owner.lifecycle)
        assertFalse(view.keepScreenOn)
        owner.registry.currentState=Lifecycle.State.RESUMED;assertTrue(view.keepScreenOn)
        owner.registry.currentState=Lifecycle.State.STARTED;assertFalse(view.keepScreenOn)
        owner.registry.currentState=Lifecycle.State.RESUMED;assertTrue(view.keepScreenOn)
        lease.close();assertFalse(view.keepScreenOn)
        view.keepScreenOn=true
        val original=ScreenAwakeLease(view,owner.lifecycle)
        original.close();assertTrue(view.keepScreenOn)
    }
    @Test fun operationGuidesHaveMatchingLanguagesAndUniqueActionableArticles() {
        val json=app.assets.open("operation-guide.json").bufferedReader().use { it.readText() }
        val zh=parseOperationGuide(json,"zh");val en=parseOperationGuide(json,"en")
        assertEquals(6,zh.size);assertEquals(zh.map {it.id},en.map {it.id})
        val za=zh.flatMap {it.articles};val ea=en.flatMap {it.articles}
        assertEquals(21,za.size);assertEquals(za.map {it.id},ea.map {it.id})
        assertEquals(za.size,za.map {it.id}.distinct().size)
        za.zip(ea).forEach { (z,e) ->
            assertNotEquals(z.title,e.title);assertEquals(z.steps.size,e.steps.size)
            assertTrue(z.steps.size in 3..5)
            assertTrue((z.steps+e.steps).none {it.isBlank()})
        }
    }
}
