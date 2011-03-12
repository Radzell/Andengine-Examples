package com.blockbreaktest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.hud.HUD;
import org.anddev.andengine.engine.handler.physics.PhysicsHandler;
import org.anddev.andengine.engine.handler.runnable.RunnableHandler;
import org.anddev.andengine.engine.handler.timer.ITimerCallback;
import org.anddev.andengine.engine.handler.timer.TimerHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.anddev.andengine.entity.particle.Particle;
import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.CircleOutlineParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.ColorInitializer;
import org.anddev.andengine.entity.particle.initializer.IParticleInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ColorModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.entity.particle.modifier.ScaleModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.physics.box2d.PhysicsConnector;
import org.anddev.andengine.extension.physics.box2d.PhysicsFactory;
import org.anddev.andengine.extension.physics.box2d.PhysicsWorld;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.font.FontFactory;
import org.anddev.andengine.opengl.texture.BuildableTexture;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.builder.BlackPawnTextureBuilder;
import org.anddev.andengine.opengl.texture.builder.ITextureBuilder.TextureSourcePackingException;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.ui.activity.BaseGameActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class MainActivity extends BaseGameActivity implements
		IOnSceneTouchListener, ContactListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 320;
	private static final int CAMERA_HEIGHT = 480;
	// ===========================================================
	// Fields
	// ===========================================================

	private BuildableTexture mBuildableTexture;
	private TextureRegion mBlockTextureRegion;
	private TextureRegion mPaddleTextureRegion;
	private TextureRegion mBallTextureRegion;
	private List<Sprite> mPaddles;
	private List<Sprite> mBalls;
	private List<Sprite> mBlocks;

	private PhysicsWorld mPhysicsWorld;
	private boolean isPlaying;
	private TextureRegion mParticleTextureRegion;
	private Font mFont;
	private int lives;
	private ChangeableText livesText;

	@Override
	public Engine onLoadEngine() {
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		final EngineOptions engineOptions = new EngineOptions(true,
				ScreenOrientation.PORTRAIT, new FillResolutionPolicy(), camera);
		engineOptions.getTouchOptions().setRunOnUpdateThread(true);
		return new Engine(engineOptions);
	}

	@Override
	public void onLoadResources() {
		mBuildableTexture = new BuildableTexture(1024, 1024,
				TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		Texture mTexture = new Texture(32, 32,
				TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		Texture mFontTexture = new Texture(512, 512,
				TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		TextureRegionFactory.setAssetBasePath("gfx/");
		mBlockTextureRegion = TextureRegionFactory.createFromAsset(
				mBuildableTexture, this, "brick.png");
		mBallTextureRegion = TextureRegionFactory.createFromAsset(mTexture,
				this, "ball.png", 0, 0);
		mPaddleTextureRegion = TextureRegionFactory.createFromAsset(
				mBuildableTexture, this, "paddle.png");
		mParticleTextureRegion = TextureRegionFactory.createFromAsset(
				mBuildableTexture, this, "particle.png");
		FontFactory.setAssetBasePath("fonts/");
		mFont = FontFactory.createFromAsset(mFontTexture, this, "akashi.ttf",
				16, true, Color.WHITE);
		try {
			mBuildableTexture.build(new BlackPawnTextureBuilder(1));
		} catch (TextureSourcePackingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.getEngine().getTextureManager()
				.loadTextures(this.mBuildableTexture, mTexture, mFontTexture);
		this.mEngine.getFontManager().loadFonts(mFont);

	}

	@Override
	public Scene onLoadScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());
		mPaddles = new ArrayList<Sprite>();
		mBalls = new ArrayList<Sprite>();
		mBlocks = new ArrayList<Sprite>();
		isPlaying = false;
		final Scene scene = new Scene(2);
		scene.setBackground(new ColorBackground(0, 0, 0));
		scene.setOnSceneTouchListener(this);

		this.mPhysicsWorld = new PhysicsWorld(new Vector2(0, 0), false);

		final Shape ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH,
				2);
		final Shape roof = new Rectangle(0, 0, CAMERA_WIDTH, 2);
		final Shape left = new Rectangle(-2, 0, 2, CAMERA_HEIGHT);
		final Shape right = new Rectangle(CAMERA_WIDTH, 0, 2, CAMERA_HEIGHT);

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0,
				0.5f, 0.5f);
		// PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground,
		// BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof,
				BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, left,
				BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, right,
				BodyType.StaticBody, wallFixtureDef);

		// scene.getFirstChild().attachChild(ground);
		scene.getFirstChild().attachChild(roof);
		scene.getFirstChild().attachChild(left);
		scene.getFirstChild().attachChild(right);

		scene.registerUpdateHandler(this.mPhysicsWorld);

		// Creating initial paddle
		Sprite paddle = new Sprite(CAMERA_WIDTH / 2, CAMERA_HEIGHT - 15,
				mPaddleTextureRegion) {
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
					float pTouchAreaLocalX, float pTouchAreaLocalY) {
				if (pSceneTouchEvent.isActionDown()) {
					launchBall();
				}
				return false;
			}
		};
		Body body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, paddle,
				BodyType.KinematicBody, wallFixtureDef);
		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(
				paddle, body, true, false));

		mPaddles.add(paddle);
		scene.getFirstChild().attachChild(paddle);
		scene.registerTouchArea(paddle);
		scene.setTouchAreaBindingEnabled(true);

		// Creating initial ball
		initBall(scene);
		this.mPhysicsWorld.setContactListener(this);

		return scene;
	}

	private void initLevel(int[][] level) {

		int posx = 0;
		int posy = 0;
		int horSpacing = 40;
		int vertSpacing = 20;
		for (int r = 0; r < level.length; r++) {
			posx = 0;
			for (int c = 0; c < level[r].length; c++) {

				if (level[r][c] == 1) {
					addBlock(posx, posy);

				}
				posx += horSpacing;

			}
			posy += vertSpacing;
		}

	}

	private void createHud() {
		HUD pHUD = new HUD();

		this.livesText = new ChangeableText(0, 0, mFont, "Lives Remaining:",
				"Lives Remaining:X".length());
		livesText.setPosition(0, 0);

		pHUD.getFirstChild().attachChild(livesText);

		this.getEngine().getCamera().setHUD(pHUD);
		changeLives(0);
	}

	private void changeLives(int life) {
		lives += life;
		if (lives == 0) {
			loseGameOver();
		}
		livesText.setText("Lives Remaining:" + lives);
	}

	private void loseGameOver() {
		final Context context = this;
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage("Game Over\nRetry?")
						.setCancelable(false)
						.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										startActivity(new Intent(
												getBaseContext(),
												MainActivity.class));
										finish();
										dialog.cancel();
									}
								})
						.setNegativeButton("No",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										finish();
									}
								});
				final AlertDialog alertDialog = builder.create();

				alertDialog.show();

			}
		});
	}

	private void addBlock(int posx, int posy) {
		Sprite block = new Sprite(posx, posy, mBlockTextureRegion);

		mBlocks.add(block);
		this.getEngine().getScene().getLastChild().attachChild(block);
		final FixtureDef FixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f,
				0.5f);
		Body body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, block,
				BodyType.StaticBody, FixtureDef);
		body.setUserData(block);
		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(block,
				body, true, false));
	}

	private void deadBall(final Sprite sprite) {
		isPlaying = false;
		changeLives(-1);
		this.runOnUpdateThread(new Runnable() {

			@Override
			public void run() {

				final Scene scene = getEngine().getScene();

				final Body facePhysicsConnector = mPhysicsWorld
						.getPhysicsConnectorManager().findBodyByShape(sprite);
				mPhysicsWorld.destroyBody(facePhysicsConnector);
				Log.i("Removing", "removing block");
				mBlocks.remove(sprite);
				scene.getFirstChild().detachChild(sprite);

			}
		});
		initBall(getEngine().getScene());
	}

	protected void launchBall() {
		if (!isPlaying) {
			isPlaying = true;

			Body ballBody = this.mPhysicsWorld.getPhysicsConnectorManager()
					.findBodyByShape(mBalls.get(0));
			ballBody.setLinearVelocity(new Vector2(0, -10));
		}

	}

	private void initBall(Scene scene) {
		Sprite mBall = new Sprite(0, 0, mBallTextureRegion) {
			@Override
			protected void onManagedUpdate(float pSecondsElapsed) {
				if (mY > CAMERA_HEIGHT + 10) {
					deadBall(this);
				}
				super.onManagedUpdate(pSecondsElapsed);
			}

			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
					float pTouchAreaLocalX, float pTouchAreaLocalY) {
				launchBall();
				return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX,
						pTouchAreaLocalY);
			}
		};
		scene.registerTouchArea(mBall);
		mBall.setPosition(mPaddles.get(0).getX()
				+ (mPaddles.get(0).getWidth() / 2) - 6,
				mPaddles.get(0).getY() - 15);
		mBalls.add(0, mBall);
		final FixtureDef FixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f,
				0.5f);
		Body body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, mBall,
				BodyType.DynamicBody, FixtureDef);
		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(mBall,
				body, true, true));
		scene.getFirstChild().attachChild(mBall);
	}

	@Override
	public void onLoadComplete() {
		lives = 3;
		int[][] level1 = { { 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 1, 1, 0, 0, 0 },
				{ 0, 0, 0, 1, 1, 0, 0, 0 }, { 0, 1, 1, 1, 1, 1, 1, 0 },
				{ 0, 1, 1, 1, 1, 1, 1, 0 }, { 0, 0, 0, 1, 1, 0, 0, 0 },
				{ 0, 0, 0, 1, 1, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0 }, };
		initLevel(level1);
		createHud();
	}

	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
		if (this.mPhysicsWorld != null) {
			if (pSceneTouchEvent.isActionDown()
					|| pSceneTouchEvent.isActionMove()) {
				for (int i = 0; i < mPaddles.size(); ++i) {
					// Add action

					float box2d_x = (pSceneTouchEvent.getX()) / 32;
					final PhysicsConnector paddlePhysicsConnector = this.mPhysicsWorld
							.getPhysicsConnectorManager()
							.findPhysicsConnectorByShape(mPaddles.get(i));
					Body paddleBody = paddlePhysicsConnector.getBody();
					paddleBody
							.setTransform(
									new Vector2(box2d_x, paddleBody
											.getPosition().y), 0);
					if (!isPlaying) {
						for (int b = 0; b < mBalls.size(); b++) {
							final PhysicsConnector bodyPhysicsConnector = this.mPhysicsWorld
									.getPhysicsConnectorManager()
									.findPhysicsConnectorByShape(mBalls.get(0));
							float box2dx = (pSceneTouchEvent.getX()) / 32;
							Body ballBody = bodyPhysicsConnector.getBody();
							ballBody.setTransform(
									new Vector2(box2dx,
											ballBody.getPosition().y), 0);

						}
					}
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void beginContact(Contact contact) {

	}

	private void removeBlock(final int i) {

		this.runOnUpdateThread(new Runnable() {

			@Override
			public void run() {
				if (i < mBlocks.size()) {
					Sprite sprite = mBlocks.get(i);
					final Scene scene = getEngine().getScene();

					final Body facePhysicsConnector = mPhysicsWorld
							.getPhysicsConnectorManager().findBodyByShape(
									sprite);
					mPhysicsWorld.destroyBody(facePhysicsConnector);
					Log.i("Removing", "removing block");
					mBlocks.remove(sprite);
					scene.getLastChild().detachChild(sprite);
				}

			}
		});

	}

	private Vector2 adjustVelocity(Vector2 velocity) {
		if (velocity.x < 0)
			velocity.x = -10;
		else
			velocity.x = 10;

		if (velocity.y < 0)
			velocity.y = -10;
		else
			velocity.y = 10;

		return velocity;
	}

	/*****************************************************************************
	 * float TogglePosNeg( int input_number)
	 * 
	 * what it does: makes a +ve number -ve, and -ve number +ve
	 *****************************************************************************/
	float TogglePosNeg(float fInputNumber) {
		return (fInputNumber - (2 * fInputNumber));
	}

	private void createParticles(float mX, float mY) {

		final Random generator = new Random();

		final float fDecelPercent = .1f / 100.0f;
		final double vel = 100;
		CircleOutlineParticleEmitter emitter = new CircleOutlineParticleEmitter(
				mX, mY, 1);

		final ParticleSystem particleSystem = new ParticleSystem(emitter, 500,
				500, 500, this.mParticleTextureRegion);
		particleSystem.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
		particleSystem.addParticleInitializer(new ColorInitializer(
				(205f / 255f), 0, 0));
		particleSystem.addParticleModifier(new ColorModifier((204f / 255f), 1f,
				0, 0, 0, 0, 0, .5f));
		particleSystem.addParticleModifier(new ScaleModifier(1f, .1f, 0, 2));
		particleSystem.addParticleModifier(new ExpireModifier(11.5f));
		particleSystem.addParticleModifier(new AlphaModifier(1.0f, 0.0f, 2.5f,
				3.5f));
		particleSystem.addParticleInitializer(new IParticleInitializer() {

			@Override
			public void onInitializeParticle(Particle pParticle) {
				int ang = generator.nextInt(359);
				float fVelocityX = (float) (Math.cos(Math.toRadians(ang)) * vel);
				float fVelocityY = (float) (Math.sin(Math.toRadians(ang)) * vel);
				PhysicsHandler physicshandler = new PhysicsHandler(pParticle);
				physicshandler.setVelocity(fVelocityX, fVelocityY);
				// calculate air resistance that acts opposite to particle
				// velocity
				float fVelXopposite = TogglePosNeg(fVelocityX);
				float fVelYopposite = TogglePosNeg(fVelocityY);
				// x% of deceleration is applied (that is oppositetovelocity)

				// physicshandler.setVelocity(fVelXopposite * fDecelPercent,
				// fVelYopposite * fDecelPercent);
				pParticle.registerUpdateHandler(physicshandler);
			}
		});

		particleSystem.addParticleModifier(new AlphaModifier(1.0f, 0.0f, 4.5f,
				11.5f));

		getEngine().getScene().getFirstChild().attachChild(particleSystem);
		this.getEngine()
				.getScene()
				.registerUpdateHandler(
						new TimerHandler(1, new ITimerCallback() {

							@Override
							public void onTimePassed(TimerHandler pTimerHandler) {
								final RunnableHandler runnableRemoveHandler = new RunnableHandler();
								getEngine().getScene().registerUpdateHandler(
										runnableRemoveHandler);

								runnableRemoveHandler
										.postRunnable(new Runnable() {
											@Override
											public void run() {
												getEngine()
														.getScene()
														.getFirstChild()
														.detachChild(
																particleSystem);
											}
										});
							}
						}));
	}

	@Override
	public void endContact(Contact contact) {
		if (isPlaying) {
			Body paddleBody = this.mPhysicsWorld.getPhysicsConnectorManager()
					.findBodyByShape(mPaddles.get(0));
			Body ballBody = this.mPhysicsWorld.getPhysicsConnectorManager()
					.findBodyByShape(mBalls.get(0));
			ballBody.setLinearVelocity(adjustVelocity(ballBody
					.getLinearVelocity()));
			Body bodyA = contact.getFixtureA().getBody();
			Body bodyB = contact.getFixtureB().getBody();

			if (bodyA.getUserData() != null) {
				int found = -1;

				for (int i = 0; i < mBlocks.size(); ++i) {
					if (bodyA.getUserData() == mBlocks.get(i)) {
						found = i;
					}
				}
				if (found != -1) {
					createParticles(mBalls.get(0).getX(), mBalls.get(0).getY());
					removeBlock(found);

				}
			}
			if (bodyB.getUserData() != null) {
				int found = -1;
				for (int i = 0; i < mBlocks.size(); ++i) {
					if (bodyB.getUserData() == mBlocks.get(i)) {
						found = i;
					}
				}
				if (found != -1) {
					createParticles(mBalls.get(0).getX(), mBalls.get(0).getY());
					removeBlock(found);

				}
			}
		}
	}

}