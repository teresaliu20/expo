package expo.modules.intentlauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.support.annotation.NonNull;

import expo.core.ExportedModule;
import expo.core.ModuleRegistry;
import expo.core.Promise;
import expo.core.arguments.ReadableArguments;
import expo.core.interfaces.ActivityEventListener;
import expo.core.interfaces.ActivityProvider;
import expo.core.interfaces.ExpoMethod;
import expo.core.interfaces.ModuleRegistryConsumer;
import expo.core.interfaces.services.UIManager;
import expo.errors.CurrentActivityNotFoundException;
import expo.errors.ModuleNotFoundException;

public class IntentLauncherModule extends ExportedModule implements ModuleRegistryConsumer {
  private static final int REQUEST_CODE = 12;
  private static final String ATTR_ACTION = "action";
  private static final String ATTR_TYPE = "type";
  private static final String ATTR_CATEGORY = "category";
  private static final String ATTR_EXTRA = "extra";
  private static final String ATTR_DATA = "data";
  private static final String ATTR_FLAGS = "flags";
  private static final String ATTR_PACKAGE_NAME = "packageName";
  private static final String ATTR_CLASS_NAME = "className";

  private UIManager mUIManager;
  private ActivityProvider mActivityProvider;

  public IntentLauncherModule(Context context) {
    super(context);
  }

  @Override
  public String getName() {
    return "ExpoIntentLauncher";
  }

  @Override
  public void setModuleRegistry(ModuleRegistry moduleRegistry) {
    mActivityProvider = moduleRegistry.getModule(ActivityProvider.class);
    mUIManager = moduleRegistry.getModule(UIManager.class);
  }

  @ExpoMethod
  public void startActivity(@NonNull ReadableArguments params, final Promise promise) {
    Activity activity = mActivityProvider != null ? mActivityProvider.getCurrentActivity() : null;

    if (activity == null) {
      promise.reject(new CurrentActivityNotFoundException());
      return;
    }
    if (mUIManager == null) {
      promise.reject(new ModuleNotFoundException("UIManager"));
      return;
    }

    Intent intent = new Intent();

    if (params.containsKey(ATTR_CLASS_NAME)) {
      ComponentName cn = params.containsKey(ATTR_PACKAGE_NAME)
          ? new ComponentName(params.getString(ATTR_PACKAGE_NAME), params.getString(ATTR_CLASS_NAME))
          : new ComponentName(getContext(), params.getString(ATTR_CLASS_NAME));

      intent.setComponent(cn);
    }
    if (params.containsKey(ATTR_ACTION)) {
      intent.setAction(params.getString(ATTR_ACTION));
    }

    // `setData` and `setType` are exclusive, so we need to use `setDateAndType` in that case.
    if (params.containsKey(ATTR_DATA) && params.containsKey(ATTR_TYPE)) {
      intent.setDataAndType(Uri.parse(params.getString(ATTR_DATA)), params.getString(ATTR_TYPE));
    } else {
      if (params.containsKey(ATTR_DATA)) {
        intent.setData(Uri.parse(params.getString(ATTR_DATA)));
      }
      if (params.containsKey(ATTR_TYPE)) {
        intent.setType(params.getString(ATTR_TYPE));
      }
    }

    if (params.containsKey(ATTR_EXTRA)) {
      intent.putExtras(params.getArguments(ATTR_EXTRA).toBundle());
    }
    if (params.containsKey(ATTR_FLAGS)) {
      intent.addFlags(params.getInt(ATTR_FLAGS));
    }
    if (params.containsKey(ATTR_CATEGORY)) {
      intent.addCategory(params.getString(ATTR_CATEGORY));
    }

    final UIManager uiManager = mUIManager;

    uiManager.registerActivityEventListener(new ActivityEventListener() {
      @Override
      public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        if (requestCode != REQUEST_CODE) {
          return;
        }

        Bundle response = new Bundle();

        response.putInt("resultCode", resultCode);

        if (intent != null) {
          Uri data = intent.getData();
          if (data != null) {
            response.putString(ATTR_DATA, data.toString());
          }

          Bundle extras = intent.getExtras();
          if (extras != null) {
            response.putBundle(ATTR_EXTRA, extras);
          }
        }
        promise.resolve(response);
        uiManager.unregisterActivityEventListener(this);
      }

      @Override
      public void onNewIntent(Intent intent) {}
    });

    activity.startActivityForResult(intent, REQUEST_CODE);
  }
}
