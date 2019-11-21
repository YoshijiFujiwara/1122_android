package local.hal.st31.android.handson_api;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String API_URL = "http://10.0.2.2:8000"; // https://developer.android.com/studio/run/emulator-networking.html
    public static final String PREFS_NAME = "MyApp_Settings"; // sharedPreference
    private static final String TOKEN_KEY = "jwt"; // sharedPreference内にJWTを保存するときに使用
    private static Context context; // sharedPreferenceにデータを格納するときに使用

    private List<Map<String, String>> _list;

    public static Context getAppContext() {
        return MainActivity.context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.context = getApplicationContext();
        setContentView(R.layout.activity_main);
        _list = createList();

        ListView lvCityList = findViewById(R.id.lvCityList);
        String[] from = {"description"};
        int[] to = {android.R.id.text1};
        SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), _list, android.R.layout.simple_expandable_list_item_1, from, to);
        lvCityList.setAdapter(adapter);
        lvCityList.setOnItemClickListener(new ListItemClickListener());
    }

    private List<Map<String, String>> createList() {
        List<Map<String, String>> list = new ArrayList<>();

        // ユーザー登録用のダミーデータ
        JSONObject registerParam = new JSONObject();
        try {
            registerParam.put("email", "hogehoge@hoge.com");
            registerParam.put("password", "secret");
        } catch (JSONException ex) {
            Log.e("Json構築失敗", ex.toString());
        }
        // ログイン用のダミーデータ
        JSONObject loginParam = new JSONObject();
        try {
            loginParam.put("email", "hogehoge@hoge.com");
            loginParam.put("password", "secret");
        } catch (JSONException ex) {
            Log.e("Json構築失敗", ex.toString());
        }
        // 記事投稿用のダミーデータ(※※※※ 日本語のtitle, bodyを書くとバグるので、英語で。対策分かったら教えて ※※※※)
        JSONObject createArticleParam = new JSONObject();
        try {
            createArticleParam.put("title", "title");
            createArticleParam.put("body", "body");
        } catch (JSONException ex) {
            Log.e("Json構築失敗", ex.toString());
        }
        // 記事更新用のダミーデータ(※※※※ 日本語のtitle, bodyを書くとバグるので、英語で。対策分かったら教えて ※※※※)
        JSONObject updateArticleParam = new JSONObject();
        try {
            updateArticleParam.put("title", "updated");
            updateArticleParam.put("body", "updated");
        } catch (JSONException ex) {
            Log.e("Json構築失敗", ex.toString());
        }

        list.add(makeMap("トークン消す", "", "", ""));
        list.add(makeMap("/register", "POST", "/register", registerParam.toString()));
        list.add(makeMap("/login", "POST", "/login", loginParam.toString()));
        list.add(makeMap("/articles", "GET", "/articles", ""));
        list.add(makeMap("/articles", "POST", "/articles", createArticleParam.toString()));
        // TODO /articles/1 などのidの数字は適当に書き換えてみてくださいね
        list.add(makeMap("/articles/{id}", "GET", "/articles/2", ""));
        list.add(makeMap("/articles/{id}", "PUT", "/articles/14", updateArticleParam.toString()));
        list.add(makeMap("/articles/{id}", "DELETE", "/articles/2", ""));

        return list;
    }

    private Map<String, String> makeMap(String title, String method, String uri, String jsonStr) {
        Map<String, String> map = new HashMap<>();
        map.put("title", title);
        map.put("method", method); // リクエストのメソッド
        map.put("uri", uri); // リクエストのURI
        map.put("description", title + " 【" + method + "】");
        map.put("json", jsonStr);

        return map;
    }

    private class ListItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Map<String, String> item = _list.get(position);
            String title = item.get("title");
            String uri = item.get("uri");
            String method = item.get("method");
            String requestJson = item.get("json");

            switch ("[" + method + "]" + uri) {
                case "トークン消す":
                    System.out.println("SharedPreferenceからトークンを消すか");
                    // SharedPreferenceからトークンを削除する
                    Context context = MainActivity.getAppContext();
                    SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.remove(TOKEN_KEY).commit(); // トークンを削除する
                    break;
                case "[POST]/register":
                    System.out.println("/register に" + method + "リクエストする");
                    new UserRegisterReceiver().execute(API_URL + uri, method, requestJson);
                    break;
                case "[POST]/login":
                    System.out.println("/login に" + method + "リクエストする");
                    new UserLoginReceiver().execute(API_URL + uri, method, requestJson);
                    break;
                case "[GET]/articles":
                    new BaseInfoReceiver().execute(API_URL + uri, method);
                    break;
                case "[POST]/articles":
                    new PostArticleReceiver().execute(API_URL + uri, method, requestJson);
                    break;
                case "[GET]/articles/{id}":
                    new BaseInfoReceiver().execute(API_URL + uri, method);
                    break;
                case "[PUT]/articles/{id}":
                    new PostArticleReceiver().execute(API_URL + uri, method, requestJson);
                    break;
                case "[DELETE]/articles/{id}":
                    new DeleteInfoReceiver().execute(API_URL + uri, method);
                    break;
                default:
                    System.out.println("ん？？");
            }
        }
    }

    private class UserRegisterReceiver extends BaseInfoReceiver {
        private static final String DEBUG_TAG = "UserRegisterReceiver";

        @Override
        protected String doInBackground(String... params) {
            String uri = params[0];
            String method = params[1];
            String jsonStr = params[2];

            HttpURLConnection con = null;
            InputStream is = null;
            String result = "";

            try {
                URL url = new URL(uri);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("Content-Type", "application/json; utf-8"); // 追記
                con.setRequestProperty("Accept", "application/json");
                con.setRequestMethod(method);

                con.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(jsonStr);
                wr.flush();
                wr.close();

                con.connect();
                is = con.getInputStream();
                result = super.is2String(is);
            } catch (MalformedURLException ex) {
                Log.e(DEBUG_TAG, "URL変換失敗", ex);
            } catch (IOException ex) {
                Log.e(DEBUG_TAG, "通信失敗", ex);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        Log.e(DEBUG_TAG, "InputStream解放失敗", ex);
                    }
                }
            }

            return result;
        }
    }

    private class UserLoginReceiver extends BaseInfoReceiver {
        private static final String DEBUG_TAG = "UserLoginReceiver";

        @Override
        protected String doInBackground(String... params) {
            String uri = params[0];
            String method = params[1];
            String jsonStr = params[2]; // リクエストで送るJSON

            HttpURLConnection con = null;
            InputStream is = null;
            String result = "";

            try {
                URL url = new URL(uri);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("Content-Type", "application/json; utf-8"); // 追記
                con.setRequestProperty("Accept", "application/json");
                con.setRequestMethod(method);
                con.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(jsonStr);
                wr.flush();
                wr.close();

                con.connect();
                is = con.getInputStream();
                result = super.is2String(is);
            } catch (MalformedURLException ex) {
                Log.e(DEBUG_TAG, "URL変換失敗", ex);
            } catch (IOException ex) {
                Log.e(DEBUG_TAG, "通信失敗", ex);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        Log.e(DEBUG_TAG, "InputStream解放失敗", ex);
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            ApiResponseDialog dialog = new ApiResponseDialog();
            Bundle extras = new Bundle();
            extras.putString("response", result);

            // ログインの時は、返却されたトークンをローカルに保存する
            // https://developer.android.com/training/data-storage/shared-preferences
            // を参考に、SharedPreferenceに保存する。
            // 少ないデータをkey-valueペアで保存するのに、いいらしいね
            try {
                JSONObject jsonObject = new JSONObject(result);
                String jwt = jsonObject.getString("token");
                Context context = MainActivity.getAppContext();
                SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(TOKEN_KEY, jwt);
                editor.commit();
            } catch (JSONException ex) {
                Log.e(DEBUG_TAG, "JSONObjectからtokenの取得に失敗しました", ex);
            }

            dialog.setArguments(extras);
            FragmentManager manager = getSupportFragmentManager();
            dialog.show(manager, "ApiResponseDialog");
        }
    }

    private class PostArticleReceiver extends BaseInfoReceiver {
        private static final String DEBUG_TAG = "PostArticleReceiver";

        @Override
        protected String doInBackground(String... params) {
            String uri = params[0];
            String method = params[1];
            String jsonStr = params[2]; // リクエストで送るJSON

            HttpURLConnection con = null;
            InputStream is = null;
            String result = "";

            try {
                URL url = new URL(uri);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("Content-Type", "application/json; utf-8"); // 追記
                con.setRequestProperty("Accept", "application/json");
                // Authorizationヘッダーにトークンを設定する
                // まず、SharedPreferenceからトークンを取り出す
                SharedPreferences sharedPref = MainActivity.getAppContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String token = sharedPref.getString(TOKEN_KEY, "");
                con.setRequestProperty("Authorization", "Bearer " + token);
                con.setRequestMethod(method);
                con.setDoOutput(true);
                // サーバーへ送るJSONをセットする
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(jsonStr);
                wr.flush();
                wr.close();

                con.connect();
                is = con.getInputStream();

                result = super.is2String(is);
            } catch (MalformedURLException ex) {
                Log.e(DEBUG_TAG, "URL変換失敗", ex);
            } catch (IOException ex) {
                Log.e(DEBUG_TAG, "通信失敗", ex);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        Log.e(DEBUG_TAG, "InputStream解放失敗", ex);
                    }
                }
            }
            return result;
        }
    }

    private class DeleteInfoReceiver extends BaseInfoReceiver {
        private static final String DEBUG_TAG = "DeleteInfoReceiver";

        @Override
        protected String doInBackground(String... params) {
            String uri = params[0];
            String method = params[1];

            HttpURLConnection con = null;
            InputStream is = null;
            String result = "";

            try {
                URL url = new URL(uri);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("Content-Type", "application/json; utf-8"); // 追記
                con.setRequestProperty("Accept", "application/json");
                // Authorizationヘッダーにトークンを設定する
                // まず、SharedPreferenceからトークンを取り出す
                SharedPreferences sharedPref = MainActivity.getAppContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String token = sharedPref.getString(TOKEN_KEY, "");
                con.setRequestProperty("Authorization", "Bearer " + token);
                con.setRequestMethod(method);
                con.setUseCaches(false);
                con.connect();
                is = con.getInputStream();

                result = super.is2String(is);
            } catch (MalformedURLException ex) {
                Log.e(DEBUG_TAG, "URL変換失敗", ex);
            } catch (IOException ex) {
                Log.e(DEBUG_TAG, "通信失敗", ex);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        Log.e(DEBUG_TAG, "InputStream解放失敗", ex);
                    }
                }
            }

            return result;
        }
    }

    private class BaseInfoReceiver extends AsyncTask<String, Void, String> {
        private static final String DEBUG_TAG = "BaseInfoReceiver";

        @Override
        protected String doInBackground(String... params) {
            String uri = params[0];
            String method = params[1];

            HttpURLConnection con = null;
            InputStream is = null;
            String result = "";

            try {
                URL url = new URL(uri);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("Content-Type", "application/json; utf-8"); // 追記
                con.setRequestProperty("Accept", "application/json");
                con.setRequestMethod(method);
                con.connect();
                is = con.getInputStream();

                result = is2String(is);
            } catch (MalformedURLException ex) {
                Log.e(DEBUG_TAG, "URL変換失敗", ex);
            } catch (IOException ex) {
                Log.e(DEBUG_TAG, "通信失敗", ex);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        Log.e(DEBUG_TAG, "InputStream解放失敗", ex);
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            ApiResponseDialog dialog = new ApiResponseDialog();
            Bundle extras = new Bundle();
            extras.putString("response", result);
            dialog.setArguments(extras);
            FragmentManager manager = getSupportFragmentManager();
            dialog.show(manager, "ApiResponseDialog");
        }

        private String is2String(InputStream is) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuffer sb = new StringBuffer();
            char[] b = new char[1024];
            int line;
            while (0 <= (line = reader.read(b))) {
                sb.append(b, 0, line);
            }
            return sb.toString();
        }
    }
}
