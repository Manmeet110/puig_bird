package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import java.util.Iterator;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class GameScreen implements Screen {
    final Bird game;
    OrthographicCamera camera;
    Stage stage;
    Player player;
    boolean dead;
    boolean isPaused;
    boolean isSoundEnabled;
    Array<Pipe> obstacles;
    long lastObstacleTime;
    float score;
    ImageButton pauseButton;
    ImageButton soundButton;
    Texture pauseTexture;
    Texture unpauseTexture;
    Texture soundEnabledTexture;
    Texture soundDisabledTexture;

    public GameScreen(final Bird gam) {
        this.game = gam;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        player = new Player();
        player.setManager(game.manager);
        stage = new Stage(new StretchViewport(800, 480, camera));
        stage.addActor(player);

        dead = false;
        isPaused = false;
        isSoundEnabled = true; // Sound effects enabled by default
        obstacles = new Array<>();
        spawnObstacle();
        score = 0;

        // Load textures for buttons
        pauseTexture = new Texture(Gdx.files.internal("pause.png"));
        unpauseTexture = new Texture(Gdx.files.internal("unpause.png"));
        soundEnabledTexture = new Texture(Gdx.files.internal("sound_enabled.png"));
        soundDisabledTexture = new Texture(Gdx.files.internal("sound_disabled.png"));

        // Create the pause button
        TextureRegionDrawable pauseDrawable = new TextureRegionDrawable(pauseTexture);
        TextureRegionDrawable unpauseDrawable = new TextureRegionDrawable(unpauseTexture);
        pauseButton = new ImageButton(pauseDrawable, unpauseDrawable);
        pauseButton.setPosition(300, 375);

        // Create the sound button
        TextureRegionDrawable soundEnabledDrawable = new TextureRegionDrawable(new TextureRegion(soundEnabledTexture));
        TextureRegionDrawable soundDisabledDrawable = new TextureRegionDrawable(new TextureRegion(soundDisabledTexture));
        soundButton = new ImageButton(soundEnabledDrawable);
        soundButton.setPosition(20, 20); // 700   300

        // Add listener to handle pause and resume
        pauseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isPaused = !isPaused;
                // Change the button image based on the game state
                if (isPaused) {
                    pauseButton.getStyle().imageUp = unpauseDrawable;
                } else {
                    pauseButton.getStyle().imageUp = pauseDrawable;
                }
            }
        });

        // Add listener to handle sound toggle
        soundButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isSoundEnabled = !isSoundEnabled;
                // Toggle sound effects based on the state of the button
                if (isSoundEnabled) {
                    soundButton.getStyle().imageUp = soundEnabledDrawable;
                } else {
                    soundButton.getStyle().imageUp = soundDisabledDrawable;
                }
            }
        });
        // Set the input processor to the stage
        Gdx.input.setInputProcessor(stage);
        // Add actors to the stage
        stage.addActor(pauseButton);
        stage.addActor(soundButton);
    }
    private void spawnObstacle() {
        float holey = MathUtils.random(50, 230);
        Pipe pipe1 = new Pipe();
        pipe1.setX(800);
        pipe1.setY(holey - 230);
        pipe1.setUpsideDown(true);
        pipe1.setManager(game.manager);
        obstacles.add(pipe1);
        stage.addActor(pipe1);

        Pipe pipe2 = new Pipe();
        pipe2.setX(800);
        pipe2.setY(holey + 200);
        pipe2.setUpsideDown(false);
        pipe2.setManager(game.manager);
        obstacles.add(pipe2);
        stage.addActor(pipe2);

        lastObstacleTime = TimeUtils.nanoTime();
    }

    @Override
    public void render(float delta) {
        // Handle touch input for bird's movement
        if (!isPaused && !dead && Gdx.input.justTouched() && !pauseButton.isPressed()) {
            player.impulso();
            if (isSoundEnabled) {
                game.manager.get("flap.wav", Sound.class).play();
            }
        }

        // Update game logic only if the game is not paused and the player is not dead
        if (!isPaused && !dead) {
            stage.act(delta);
            camera.update();

            score += Gdx.graphics.getDeltaTime();

            if (TimeUtils.nanoTime() - lastObstacleTime > 1500000000) {
                spawnObstacle();
            }

            Iterator<Pipe> iter = obstacles.iterator();
            while (iter.hasNext()) {
                Pipe pipe = iter.next();
                if (pipe.getBounds().overlaps(player.getBounds())) {
                    dead = true;
                }
            }

            iter = obstacles.iterator();
            while (iter.hasNext()) {
                Pipe pipe = iter.next();
                if (pipe.getX() < -64) {
                    obstacles.removeValue(pipe, true);
                }
            }

            if (dead) {
                game.lastScore = (int) score;
                if (game.lastScore > game.topScore) {
                    game.topScore = game.lastScore;
                }
                if (isSoundEnabled) {
                    game.manager.get("fail.wav", Sound.class).play();
                }
                game.setScreen(new GameOverScreen(game));
                dispose();
            }
        }

        // Render the game screen
        ScreenUtils.clear(0, 0, 0.2f, 1);
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        game.batch.draw(game.manager.get("background.png", Texture.class), 0, 0);
        game.batch.end();

        stage.getBatch().setProjectionMatrix(camera.combined);
        stage.draw();

        game.batch.begin();
        game.smallFont.draw(game.batch, "Score: " + (int) score, 10, 470);
        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void show() {}

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        stage.dispose();
        pauseTexture.dispose();
        unpauseTexture.dispose();
        soundEnabledTexture.dispose();
        soundDisabledTexture.dispose();
    }
}
