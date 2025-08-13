package com.example.tripsync_wear_app.data;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import java.util.List;

public class DataLayerHelper {
    private static final String TAG = "DataLayer";
    private static final String PATH_PING = "/tripsync/ping";
    private static final String PATH_PULL = "/tripsync/pull";

    public static void pull(Context ctx) {
        new Thread(() -> {
                        for (int attempt = 1; attempt <= 3; attempt++) {
                               try {
                                        List<Node> nodes = Tasks.await(Wearable.getNodeClient(ctx).getConnectedNodes());
                                       if (nodes == null || nodes.isEmpty()) {
                                                Log.d(TAG, "No connected phone nodes (attempt " + attempt + ")");
                                                Thread.sleep(400); // phone/Wear might still be negotiating connection
                                                continue;
                                            }
                                        Node target = nodes.get(0);
                                        MessageClient mc = Wearable.getMessageClient(ctx);
                                        Log.d(TAG, "sendMessage path=" + PATH_PING + " to=" + target.getId());
                                        mc.sendMessage(target.getId(), PATH_PING, new byte[0]);
                                       Log.d(TAG, "sendMessage path=" + PATH_PULL + " to=" + target.getId());
                                        mc.sendMessage(target.getId(), PATH_PULL, new byte[0]);
                                        return; // success
                                    } catch (Exception e) {
                                        Log.e(TAG, "pull failed (attempt " + attempt + ")", e);
                                    }
                        }
                    }).start();
    }
}
