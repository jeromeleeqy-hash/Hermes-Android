package com.qingyu.hermescompanion.ui.format

import com.qingyu.hermescompanion.model.HermesProject
import org.junit.Assert.*
import org.junit.Test

class ProjectSelectionTest {
    private val parent = HermesProject("parent", "Parent", "/projects")
    private val child = HermesProject("child", "Child", "/projects/app")
    @Test fun nestedProjectWinsRegardlessOfCatalogOrder() {
        assertEquals(child, projectForWorkspace(listOf(parent, child), "/projects/app/src"))
        assertEquals(child, projectForWorkspace(listOf(child, parent), "/projects/app/"))
    }
    @Test fun similarlyNamedSiblingDoesNotMatch() {
        assertNull(projectForWorkspace(listOf(child), "/projects/application"))
    }
    @Test fun rootAndAdditionalFolderAreRecognized() {
        val root = HermesProject("root", "Root", "/")
        val other = child.copy(paths = listOf("/other"))
        assertEquals(root, projectForWorkspace(listOf(root), "/"))
        assertEquals(other, projectForWorkspace(listOf(root, other), "/other/src"))
        assertNull(projectForWorkspace(listOf(root), ""))
    }
}
