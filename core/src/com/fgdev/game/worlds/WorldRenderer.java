package com.fgdev.game.worlds;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.fgdev.game.entitiles.enemies.Zombie;
import com.fgdev.game.entitiles.tiles.*;
import com.fgdev.game.screens.GameOverOverlay;
import com.fgdev.game.utils.Assets;
import com.fgdev.game.Constants;
import com.fgdev.game.utils.GamePreferences;
import com.fgdev.game.utils.ValueManager;

import static com.fgdev.game.Constants.PPM;

public class WorldRenderer implements Disposable {

    private static final String TAG = WorldRenderer.class.getName();

    private OrthographicCamera camera;
    private OrthographicCamera cameraGUI;
    private SpriteBatch batch;
    private WorldController worldController;
    private Viewport gamePort;
    private GameOverOverlay gameOverOverlay;

    // Shader
    private ShaderProgram shaderMonochrome;
    private boolean isDebug;

    public WorldRenderer(WorldController worldController) {
        this.worldController = worldController;
        isDebug = GamePreferences.instance.debug;
        init();
    }

    private void init() {
        // Init batch
        batch = new SpriteBatch();
        // Create cam used to follow mario through cam world
        camera = new OrthographicCamera();
        // Create a FitViewport to maintain virtual aspect ratio despite screen size
        gamePort = new FitViewport(Constants.V_WIDTH / PPM, Constants.V_HEIGHT / PPM, camera);
        //initially set our gamcam to be centered correctly at the start of
        camera.position.set(gamePort.getWorldWidth() / 2, gamePort.getWorldHeight() / 2, 0);
        // Camera gui
        cameraGUI = new OrthographicCamera(Constants.VIEWPORT_GUI_WIDTH,
                Constants.VIEWPORT_GUI_HEIGHT);
        cameraGUI.position.set(0, 0, 0);
        cameraGUI.setToOrtho(true); // flip y-axis
        cameraGUI.update();
        // Shader
        shaderMonochrome = new ShaderProgram(
                Gdx.files.internal(Constants.shaderMonochromeVertex),
                Gdx.files.internal(Constants.shaderMonochromeFragment));
        if (!shaderMonochrome.isCompiled()) {
            String msg = "Could not compile shader program: "
                    + shaderMonochrome.getLog();
            throw new GdxRuntimeException(msg);
        }
        // Game over overlay
        gameOverOverlay = new GameOverOverlay(batch);
    }

    public void render() {
        renderWorld(batch);
        renderTile(batch);
        renderObject(batch);
        renderGui(batch);
        renderShader(batch);
        if (isDebug) renderDebug();
    }

    private void renderShader(SpriteBatch batch) {
        batch.begin();
        if (GamePreferences.instance.useMonochromeShader) {
            batch.setShader(shaderMonochrome);
            shaderMonochrome.setUniformf("u_amount", 1.0f);
        }
        batch.setShader(null);
        batch.end();
    }

    private void renderTile(SpriteBatch batch) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // Crates
        for (Crate crate: worldController.creator.getCrates())
                crate.draw(batch);
        // Coins
        for (Coin coin: worldController.creator.getCoins())
            coin.draw(batch);
        // Feathers
        for (Feather feather: worldController.creator.getFeathers())
            feather.draw(batch);
        // Zombies
        for (Zombie zombie: worldController.creator.getZombies())
            zombie.draw(batch);
        batch.end();
    }

    private void renderObject(SpriteBatch batch) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        worldController.player.draw(batch);
        worldController.clouds.render(batch);
        batch.end();
    }

    private void renderWorld (SpriteBatch batch) {
        batch.setProjectionMatrix(camera.combined);
        //attach our game camera to our players.x coordinate
        camera.position.x = worldController.player.body.getPosition().x;
        // update our camera with correct coordinates after changes
        camera.update();
        // set the TiledMapRenderer view based on what the
        // camera sees, and render the map
        worldController.renderer.setView(camera);
        worldController.renderer.render();
    }

    private void renderGui (SpriteBatch batch) {
        batch.setProjectionMatrix(cameraGUI.combined);
        batch.begin();
        // draw collected gold coins icon + text
        // (anchored to top left edge)
        renderGuiScore(batch);
        // draw extra lives icon + text (anchored to top right edge)
        renderGuiExtraLive(batch);
        // draw FPS text (anchored to bottom right edge)
        if (GamePreferences.instance.showFpsCounter)
            renderGuiFpsCounter(batch);
        // draw Game Over
        renderGuiGameOverMessage(batch);
        // draw collected feather icon (anchored to top left edge)
        renderGuiFeatherPowerup(batch);
        batch.end();
    }

    private void renderDebug() {
        //render our Box2DDebugLines
        worldController.b2dr.render(worldController.world, camera.combined);
    }

    public void resize(int width, int height) {
        // updated our game viewport
        gamePort.update(width, height);
        cameraGUI.viewportHeight = Constants.VIEWPORT_GUI_HEIGHT;
        cameraGUI.viewportWidth = (Constants.VIEWPORT_GUI_HEIGHT
                / (float)height) * (float)width;
        cameraGUI.position.set(cameraGUI.viewportWidth / 2,
                cameraGUI.viewportHeight / 2, 0);
        cameraGUI.update();
    }


    private void renderGuiScore (SpriteBatch batch) {
        float x = -15;
        float y = -15;
        float offsetX = 50;
        float offsetY = 50;
        if (ValueManager.instance.scoreVisual < ValueManager.instance.score) {
            long shakeAlpha = System.currentTimeMillis() % 360;
            float shakeDist = 1.5f;
            offsetX += MathUtils.sinDeg(shakeAlpha * 2.2f) * shakeDist;
            offsetY += MathUtils.sinDeg(shakeAlpha * 2.9f) * shakeDist;
        }
        batch.draw(Assets.instance.goldCoin.goldCoin, x, y, offsetX,
                offsetY, 100, 100, 0.35f, -0.35f, 0);
        Assets.instance.fonts.textFontNormal.draw(batch,
                "" + (int) ValueManager.instance.scoreVisual,
                x + 75, y + 40);
    }

    private void renderGuiExtraLive (SpriteBatch batch) {
        float x = cameraGUI.viewportWidth - 50 - Constants.LIVES_START * 50;
        float y = -15;
        for (int i = 0; i < Constants.LIVES_START; i++) {
            if (ValueManager.instance.lives <= i)
                batch.setColor(0.5f, 0.5f, 0.5f, 0.5f);
            batch.draw(Assets.instance.player.head,
                    x + i * 50, y, 50, 50, 120, 120, 0.35f, -0.35f, 0);
            batch.setColor(1, 1, 1, 1);
        }
        if (ValueManager.instance.lives >= 0
                && ValueManager.instance.livesVisual > ValueManager.instance.lives) {
            int i = ValueManager.instance.lives;
            float alphaColor = Math.max(0, ValueManager.instance.livesVisual
                    - ValueManager.instance.lives - 0.5f);
            float alphaScale = 0.35f * (2 + ValueManager.instance.lives
                    - ValueManager.instance.livesVisual) * 2;
            float alphaRotate = -45 * alphaColor;
            batch.setColor(1.0f, 0.7f, 0.7f, alphaColor);
            batch.draw(Assets.instance.player.head,
                    x + i * 50, y, 50, 50, 120, 120, alphaScale, -alphaScale,
                    alphaRotate);
            batch.setColor(1, 1, 1, 1);
        }
    }

    private void renderGuiFpsCounter (SpriteBatch batch) {
        float x = cameraGUI.viewportWidth - 55;
        float y = cameraGUI.viewportHeight - 15;
        int fps = Gdx.graphics.getFramesPerSecond();
        BitmapFont fpsFont = Assets.instance.fonts.defaultNormal;
        if (fps >= 45) {
            // 45 or more FPS show up in green
            fpsFont.setColor(0, 1, 0, 1);
        } else if (fps >= 30) {
            // 30 or more FPS show up in yellow
            fpsFont.setColor(1, 1, 0, 1);
        } else {
            // less than 30 FPS show up in red
            fpsFont.setColor(1, 0, 0, 1);
        }
        fpsFont.draw(batch, "FPS: " + fps, x, y);
        fpsFont.setColor(1, 1, 1, 1); // white
    }

    private void renderGuiGameOverMessage (SpriteBatch batch) {
        if (ValueManager.instance.isGameOver()) {
            gameOverOverlay.render(Gdx.graphics.getDeltaTime());
        }
    }

    private void renderGuiFeatherPowerup (SpriteBatch batch) {
        float x = -15;
        float y = 30;
        float timeLeftFeatherPowerup =
                worldController.player.timeLeftFeatherPowerup;
        if (timeLeftFeatherPowerup > 0) {
            // Start icon fade in/out if the left power-up time
            // is less than 4 seconds. The fade interval is set
            // to 5 changes per second.
            if (timeLeftFeatherPowerup < 4) {
                if (((int)(timeLeftFeatherPowerup * 5) % 2) != 0) {
                    batch.setColor(1, 1, 1, 0.5f);
                }
            }
            batch.draw(Assets.instance.feather.feather,
                    x, y, 50, 50, 100, 100, 0.35f, -0.35f, 0);
            batch.setColor(1, 1, 1, 1);
            Assets.instance.fonts.textFontSmall.draw(batch,
                    "" + (int)timeLeftFeatherPowerup, x + 60, y + 57);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shaderMonochrome.dispose();
        gameOverOverlay.dispose();
//        // Grounds
//        for (Ground ground: worldController.creator.getGrounds())
//            ground.dispose();
//        // Signs
//        for (Sign sign: worldController.creator.getSigns())
//            sign.dispose();
//        // Crates
//        for (Crate crate: worldController.creator.getCrates())
//            crate.dispose();
//        // Coins
//        for (Coin coin: worldController.creator.getCoins())
//            coin.dispose();
//        // Feathers
//        for (Feather feather: worldController.creator.getFeathers())
//            feather.dispose();
//        // Zombies
//        for (Zombie zombie: worldController.creator.getZombies())
//            zombie.dispose();
    }
}