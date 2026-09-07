package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.*
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

class ArtifactFilesTest {
    private val client = mock(HermesApiClient::class.java)
    private val item = RecentArtifact("sales", "source", "日报", "m1", "/wrong/报告.md", "报告.md", "Markdown", "/work/sales")
    private fun document(path: String) = WorkspaceDocument("报告.md", path, "text/markdown", "# 日报")

    @Test fun parserKeepsWholeLinksExtensionsAndEncodedChineseNames() {
        val text = "[报告](reports/%E6%8A%A5%E5%91%8A.md) [文档](./result.docx) [网页](/work/page.html) https://example.com/file.pdf"
        assertEquals(listOf("reports/报告.md", "./result.docx", "/work/page.html"), ChatInsightParser.artifactsFromText(text).map { it.path })
    }
    @Test fun parserDoesNotCreateAbsoluteDuplicatesInsideRelativeLinks() {
        assertEquals(listOf("outputs/a+b.md"), ChatInsightParser.artifactsFromText("[报告](outputs/a+b.md)").map { it.path })
        assertEquals(listOf("/work/报告.docx", "/work/预览.html"), ChatInsightParser.artifactsFromText("`/work/报告.docx` 和 `/work/预览.html`").map { it.path })
    }
    @Test fun pathsHandleRootHomeAndFileUriWithoutInventingDirectoryForUrls() {
        assertEquals("/report.md", resolveRemoteArtifactPath("report.md", "/"))
        assertEquals("~/日报.md", resolveRemoteArtifactPath("~/日报.md", "/work"))
        assertEquals("/work/a+b.md", resolveRemoteArtifactPath("file:///work/a%2Bb.md", "/ignored"))
        assertEquals("/work/日报.md", resolveRemoteArtifactPath("sandbox:/work/%E6%97%A5%E6%8A%A5.md", ""))
        assertNull(resolveRemoteArtifactPath("https://example.com/report.md", "/work"))
    }
    @Test fun originalRelativePathUsesSourceConversationAndExplicitProfile() {
        `when`(client.readWorkspaceDocumentForProfile("/source/reports/报告.md", "sales")).thenReturn(document("/source/reports/报告.md"))
        val result = ArtifactFileReader(client).read(item.copy(sourcePath = "reports/报告.md", workspacePath = ""), HermesSession("source", "日报", profile="sales", workspacePath="/source"), emptyList())
        assertEquals("/source/reports/报告.md", result.path)
        verify(client, never()).readWorkspaceDocument(anyString())
    }
    @Test fun oldBrokenIndexRecoversOnlyFromItsSourceMessage() {
        `when`(client.readWorkspaceDocumentForProfile(item.path,"sales")).thenAnswer { throw ApiException(404,"Path not found") }
        `when`(client.readWorkspaceDocumentForProfile("/work/sales/reports/报告.md","sales")).thenReturn(document("/work/sales/reports/报告.md"))
        val result=ArtifactFileReader(client).read(item,null,listOf(ChatMessage(id="m1",role=MessageRole.ASSISTANT,content="[报告](reports/报告.md)"),ChatMessage(id="m2",role=MessageRole.ASSISTANT,content="[报告](/other/报告.md)")))
        assertEquals("/work/sales/reports/报告.md",result.path)
        verify(client,never()).loadArtifactSourceMessages(anyString(),anyString(),anyString())
        verify(client,never()).readWorkspaceDocumentForProfile("/other/报告.md","sales")
    }
    @Test fun missingFileKeepsClearErrorAndNeverSearchesOtherProjects() {
        `when`(client.readWorkspaceDocumentForProfile(item.path,"sales")).thenAnswer { throw ApiException(404,"Path not found") }
        `when`(client.loadArtifactSourceMessages("source","sales","m1")).thenReturn(emptyList())
        val error=assertThrows(ApiException::class.java) { ArtifactFileReader(client).read(item,null,listOf(ChatMessage(id="other",role=MessageRole.ASSISTANT,content="[报告](/other/报告.md)"))) }
        assertTrue(error.message.contains("移动或删除"))
        verify(client,never()).readWorkspaceDocumentForProfile("/other/报告.md","sales")
    }
    @Test fun permissionFailureDoesNotTryAlternatePaths() {
        `when`(client.readWorkspaceDocumentForProfile(item.path,"sales")).thenAnswer { throw ApiException(403,"denied") }
        val error=assertThrows(ApiException::class.java) { ArtifactFileReader(client).read(item,null,emptyList()) }
        assertEquals(403,error.statusCode)
        verify(client,never()).loadArtifactSourceMessages(anyString(),anyString(),anyString())
    }
    @Test fun textImageAndBinaryFilesBecomeUsableAttachments() {
        val text=AttachmentReader.fromWorkspaceDocument(document("/work/报告.md"))
        assertEquals("# 日报",text.textContent)
        val image=AttachmentReader.fromWorkspaceDocument(WorkspaceDocument("photo.png","/work/photo.png","image/png","",byteArrayOf(1,2,3)))
        assertEquals("data:image/png;base64,AQID",image.dataUrl)
        val pdf=AttachmentReader.fromWorkspaceDocument(WorkspaceDocument("a.pdf","/work/a.pdf","application/pdf","",byteArrayOf(4,5)))
        assertEquals("/work/a.pdf",pdf.remotePath)
        assertNull(pdf.dataUrl);assertNull(pdf.textContent)
        assertEquals("",AttachmentReader.fromWorkspaceDocument(document("/empty.md").copy(content="",bytes=byteArrayOf())).textContent)
    }
    @Test fun storedArtifactKeepsItsOriginalDirectoryAfterSourceSessionMoves() {
        `when`(client.readWorkspaceDocumentForProfile("/work/sales/reports/报告.md","sales")).thenReturn(document("/work/sales/reports/报告.md"))
        val result=ArtifactFileReader(client).read(item.copy(sourcePath="reports/报告.md"),HermesSession("source","日报",profile="sales",workspacePath="/new-project"),emptyList())
        assertEquals("/work/sales/reports/报告.md",result.path)
        verify(client,never()).readWorkspaceDocumentForProfile("/new-project/reports/报告.md","sales")
    }
    @Test fun staleRootRecoversExactRelativeLinkInUpdatedSourceDirectory() {
        `when`(client.readWorkspaceDocumentForProfile(item.path,"sales")).thenAnswer { throw ApiException(404,"missing") }
        `when`(client.readWorkspaceDocumentForProfile("/work/sales/reports/报告.md","sales")).thenAnswer { throw ApiException(404,"missing") }
        `when`(client.readWorkspaceDocumentForProfile("/new-project/reports/报告.md","sales")).thenReturn(document("/new-project/reports/报告.md"))
        val result=ArtifactFileReader(client).read(item,HermesSession("source","日报",profile="sales",workspacePath="/new-project"),listOf(ChatMessage(id="m1",role=MessageRole.ASSISTANT,content="[报告](reports/报告.md)")))
        assertEquals("/new-project/reports/报告.md",result.path)
    }

}
