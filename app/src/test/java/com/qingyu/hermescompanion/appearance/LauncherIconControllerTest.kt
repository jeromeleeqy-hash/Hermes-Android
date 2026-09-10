package com.qingyu.hermescompanion.appearance

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import com.qingyu.hermescompanion.HermesHostActivity
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class LauncherIconControllerTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private val pm get()=context.packageManager
    private fun component(icon:LauncherIcon)=ComponentName(context.packageName,icon.alias)
    @Before fun defaults() {
        context.getSharedPreferences("launcher_appearance",0).edit().clear().commit()
        LauncherIcon.entries.forEach { pm.setComponentEnabledSetting(component(it),PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,PackageManager.DONT_KILL_APP) }
    }
    @Test fun aliasChoicePersistsWithoutDisablingTheShareActivity() {
        val controller=LauncherIconController(context)
        assertEquals(LauncherIcon.PARTNER,controller.current())
        assertEquals("com.qingyu.hermescompanion.MainActivity",LauncherIcon.PARTNER.alias)
        assertEquals(HermesHostActivity::class.java.name,pm.getActivityInfo(component(LauncherIcon.PARTNER),0).targetActivity)
        assertTrue(controller.select(LauncherIcon.SPRITE).isSuccess)
        assertEquals(LauncherIcon.SPRITE,LauncherIconController(context).reconcile())
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED,pm.getComponentEnabledSetting(component(LauncherIcon.SPRITE)))
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED,pm.getComponentEnabledSetting(component(LauncherIcon.PARTNER)))
        assertNotEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED,pm.getComponentEnabledSetting(ComponentName(context,HermesHostActivity::class.java)))
        val shared=pm.queryIntentActivities(Intent(Intent.ACTION_SEND).setType("text/plain").setPackage(context.packageName),0)
        assertTrue(shared.any { it.activityInfo.name == HermesHostActivity::class.java.name })
        val launchers=pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName),0)
        assertEquals(listOf(LauncherIcon.SPRITE.alias),launchers.map { it.activityInfo.name })
        assertTrue(controller.select(LauncherIcon.PARTNER).isSuccess)
    }
    @Test @Config(sdk=[28]) fun legacyAndroidKeepsExactlyOneLauncher()=aliasChoicePersistsWithoutDisablingTheShareActivity()
    @Test @Config(sdk=[26,28,35]) fun bothLauncherDrawablesInflateOnSupportedAndroidVersions() {
        for(id in listOf(com.qingyu.hermescompanion.R.mipmap.ic_launcher,com.qingyu.hermescompanion.R.mipmap.ic_launcher_sprite)) {
            val drawable=context.getDrawable(id)
            assertTrue(drawable is android.graphics.drawable.AdaptiveIconDrawable)
            assertNotNull((drawable as android.graphics.drawable.AdaptiveIconDrawable).foreground)
            assertNotNull(drawable.background)
        }
    }
    @Test fun damagedAliasStateIsRepairedUsingSavedChoice() {
        val controller=LauncherIconController(context)
        assertTrue(controller.select(LauncherIcon.SPRITE).isSuccess)
        LauncherIcon.entries.forEach { pm.setComponentEnabledSetting(component(it),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP) }
        assertEquals(LauncherIcon.SPRITE,controller.reconcile())
        LauncherIcon.entries.forEach { pm.setComponentEnabledSetting(component(it),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP) }
        assertEquals(LauncherIcon.SPRITE,controller.reconcile())
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED,pm.getComponentEnabledSetting(component(LauncherIcon.PARTNER)))
    }

    @Test @Config(sdk=[28]) fun legacyFailureRestoresPreviousIconAndNeverDisablesTheRealActivity() {
        val fake=org.mockito.Mockito.mock(PackageManager::class.java)
        val states=mutableMapOf<String,Int>()
        val changed=mutableListOf<String>()
        var fail=true
        org.mockito.Mockito.`when`(fake.getComponentEnabledSetting(org.mockito.ArgumentMatchers.any(ComponentName::class.java)))
            .thenAnswer { states[it.getArgument<ComponentName>(0).className] ?: PackageManager.COMPONENT_ENABLED_STATE_DEFAULT }
        org.mockito.Mockito.doAnswer { call ->
            val c=call.getArgument<ComponentName>(0);val state=call.getArgument<Int>(1)
            changed+=c.className
            if(c.className==LauncherIcon.PARTNER.alias && state==PackageManager.COMPONENT_ENABLED_STATE_DISABLED && fail) {
                fail=false;throw IllegalStateException("Simulated OEM failure")
            }
            states[c.className]=state;null
        }.`when`(fake).setComponentEnabledSetting(org.mockito.ArgumentMatchers.any(ComponentName::class.java),org.mockito.ArgumentMatchers.anyInt(),org.mockito.ArgumentMatchers.anyInt())
        val wrapped=object:android.content.ContextWrapper(context) {
            override fun getApplicationContext():android.content.Context=this
            override fun getPackageManager():PackageManager=fake
        }
        val controller=LauncherIconController(wrapped)
        assertTrue(controller.select(LauncherIcon.SPRITE).isFailure)
        assertEquals(LauncherIcon.PARTNER,controller.current())
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED,states[LauncherIcon.PARTNER.alias])
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED,states[LauncherIcon.SPRITE.alias])
        assertTrue(changed.all { it in LauncherIcon.entries.map { it.alias } })
    }
}
