package com.shoppilist.presentation

import com.shoppilist.shared.presentation.VoiceViewModel
import com.shoppilist.shared.voice.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class VoiceViewModelTest {
    private val processor: VoiceIntentProcessor = mock()
    private val executor: CommandExecutor = mock()

    private val viewModel = VoiceViewModel(processor, executor)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `processText success updates result`() = runBlocking {
        val intent = VoiceIntent.CreateList("MyList")
        whenever(processor.process("Create MyList")).thenReturn(VoiceIntentResult.Success(intent, "Create MyList"))
        whenever(executor.execute(intent)).thenReturn(ExecutionResult.Success("ok"))

        viewModel.processText("Create MyList")

        // Wait a short while for coroutine to run (in real test use runTest)
        Thread.sleep(100)
        val res = viewModel.result.value
        assertNotNull(res)
        assertTrue(res!!.contains("created", ignoreCase = true) || res.contains("ok", ignoreCase = true) )
    }

    @Test
    fun `processText parse failure updates result`() = runBlocking {
        whenever(processor.process("random")).thenReturn(VoiceIntentResult.Failure("parse_error", "random"))

        viewModel.processText("random")
        Thread.sleep(100)
        val res = viewModel.result.value
        assertNotNull(res)
        assertTrue(res!!.startsWith("Parse error") || res.contains("error", ignoreCase = true))
    }
}

