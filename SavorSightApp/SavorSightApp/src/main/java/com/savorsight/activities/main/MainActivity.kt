package com.savorsight.activities.main

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.savorsight.input.BareGlassesInputDispatcher
import com.savorsight.input.LocalBareGlassesInputDispatcher
import com.savorsight.input.rememberBareGlassesInputDispatcher
import com.savorsight.savorsight.SavorSightCaptureViewModel
import com.savorsight.savorsight.LearningScreen
import com.savorsight.savorsight.RecipeViewModel
import com.savorsight.savorsight.RecipeListScreen
import com.savorsight.savorsight.RecipeDetailScreen
import com.savorsight.savorsight.RecipeDraftScreen
import com.savorsight.savorsight.CookingGuideScreen
import com.savorsight.savorsight.CheckViewModel
import com.savorsight.savorsight.CheckCameraScreen
import com.savorsight.savorsight.CheckResultScreen
import com.savorsight.navigation.BareSceneRoutes
import com.savorsight.ui.design.GlassesDisplayFrame
import com.savorsight.ui.theme.GlassesBareDevSampleTheme
import com.savorsight.ui.theme.PitchBlack

class MainActivity : ComponentActivity() {
    private val savorsightViewModel by viewModels<SavorSightCaptureViewModel>()
    private val recipeViewModel by viewModels<RecipeViewModel>()
    private val checkViewModel by viewModels<CheckViewModel>()

    private var keyDispatcher: BareGlassesInputDispatcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFullscreen()
        setContent {
            val context = LocalContext.current
            val dispatcher = rememberBareGlassesInputDispatcher(context)
            remember { keyDispatcher = dispatcher }
            GlassesBareDevSampleTheme {
                CompositionLocalProvider(LocalBareGlassesInputDispatcher provides dispatcher) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PitchBlack),
                    ) {
                        GlassesDisplayFrame {
                            SavorSightNavApp(
                                savorsightViewModel = savorsightViewModel,
                                recipeViewModel = recipeViewModel,
                                checkViewModel = checkViewModel,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> return true
            KeyEvent.KEYCODE_BACK -> {
                keyDispatcher?.dispatchBackKey()
                return true
            }
            KeyEvent.KEYCODE_PROG_BLUE -> {
                keyDispatcher?.dispatchLongKey()
                return true
            }
            KeyEvent.KEYCODE_SETTINGS -> {
                keyDispatcher?.consumeSystemKey("Key·SETTINGS")
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event != null && event.repeatCount == 0) {
            keyDispatcher?.dispatchEnterKey()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        keyDispatcher?.unregister(applicationContext)
        keyDispatcher = null
        super.onDestroy()
    }

    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun SavorSightNavApp(
    savorsightViewModel: SavorSightCaptureViewModel,
    recipeViewModel: RecipeViewModel,
    checkViewModel: CheckViewModel,
) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = BareSceneRoutes.RECIPE_LIST,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(BareSceneRoutes.RECIPE_LIST) {
            RecipeListScreen(
                onBack = { },
                onStartLearning = {
                    nav.navigate(BareSceneRoutes.LEARNING)
                },
                onSelectRecipe = { recipeId ->
                    nav.navigate("${BareSceneRoutes.RECIPE_DETAIL}/$recipeId")
                },
                viewModel = recipeViewModel,
            )
        }
        composable(BareSceneRoutes.LEARNING) {
            LearningScreen(
                onBack = { nav.popBackStack() },
                onRecipeReady = { recipeId ->
                    recipeViewModel.loadRecipeDraft(recipeId)
                    nav.navigate(BareSceneRoutes.RECIPE_DRAFT)
                },
                viewModel = savorsightViewModel,
            )
        }
        composable("${BareSceneRoutes.RECIPE_DETAIL}/{recipeId}") { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
            RecipeDetailScreen(
                onBack = { nav.popBackStack() },
                onStartCooking = {
                    recipeViewModel.startCooking {
                        nav.navigate(BareSceneRoutes.COOKING_GUIDE)
                    }
                },
                recipeId = recipeId,
                viewModel = recipeViewModel,
            )
        }
        composable(BareSceneRoutes.RECIPE_DRAFT) {
            RecipeDraftScreen(
                onBack = { nav.popBackStack() },
                onConfirm = {
                    recipeViewModel.confirmRecipeDraft(
                        onConfirmed = {
                            nav.popBackStack(BareSceneRoutes.RECIPE_LIST, false)
                        }
                    )
                },
                viewModel = recipeViewModel,
            )
        }
        composable(BareSceneRoutes.COOKING_GUIDE) {
            CookingGuideScreen(
                onBack = {
                    recipeViewModel.finishCooking()
                    nav.popBackStack(BareSceneRoutes.RECIPE_LIST, false)
                },
                onCheck = { stepIndex, stepId, stepTitle, targetState ->
                    checkViewModel.setContext(
                        recipeId = recipeViewModel.detailState.value.recipe?.recipeId ?: "",
                        stepId = stepId,
                        stepTitle = stepTitle,
                        targetState = targetState,
                    )
                    nav.navigate(BareSceneRoutes.CHECK_CAMERA)
                },
                viewModel = recipeViewModel,
            )
        }
        composable(BareSceneRoutes.CHECK_CAMERA) {
            CheckCameraScreen(
                onBack = { nav.popBackStack() },
                onResult = { nav.navigate(BareSceneRoutes.CHECK_RESULT) },
                viewModel = checkViewModel,
            )
        }
        composable(BareSceneRoutes.CHECK_RESULT) {
            CheckResultScreen(
                onBack = {
                    checkViewModel.retry()
                    nav.popBackStack(BareSceneRoutes.COOKING_GUIDE, false)
                },
                viewModel = checkViewModel,
            )
        }
    }
}
