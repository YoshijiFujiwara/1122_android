package local.hal.st31.android.handson_api;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.apache.commons.text.StringEscapeUtils;

public class ApiResponseDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle extras = getArguments();
        String response = extras.getString("response");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("レスポンス内容");
        // StringEscapeUtilsライブラリを使用するために、
        // implementation 'org.apache.commons:commons-text:1.6'
        // を追加しています
        builder.setMessage(StringEscapeUtils.unescapeJava(response));
        builder.setPositiveButton("OK", new DialogButtonClickListener());
        AlertDialog dialog = builder.create();

        return dialog;
    }

    private class DialogButtonClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {

        }
    }
}

