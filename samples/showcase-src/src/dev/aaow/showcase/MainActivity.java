package dev.aaow.showcase;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private int clickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvDevice = findViewById(R.id.tv_device_info);
        EditText etNote = findViewById(R.id.et_note);
        Button btnSave = findViewById(R.id.btn_save);
        Button btnDialog = findViewById(R.id.btn_dialog);
        Button btnCanvas = findViewById(R.id.btn_canvas_lab);
        Switch swBoost = findViewById(R.id.sw_boost);
        CheckBox cbToast = findViewById(R.id.cb_toast);
        SeekBar sbPower = findViewById(R.id.sb_power);
        TextView tvScale = findViewById(R.id.tv_scale_label);
        TextView tvStatus = findViewById(R.id.tv_saved_status);

        String info = "Model   : " + Build.MODEL + "\n"
                + "Android : " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n"
                + "ABI     : " + Build.SUPPORTED_ABIS[0] + " • Package: " + getPackageName();
        tvDevice.setText(info);

        SharedPreferences prefs = getSharedPreferences("showcase_prefs", MODE_PRIVATE);
        String savedNote = prefs.getString("last_note", "Xin chào từ APK chạy trực tiếp trên Windows!");
        int savedPower = prefs.getInt("power_level", 75);
        etNote.setText(savedNote);
        sbPower.setProgress(savedPower);
        tvScale.setText("Mức xung nhịp kết xuất: " + savedPower + "%");
        tvStatus.setText("Ghi chú đã lưu: \"" + savedNote + "\"");

        btnSave.setOnClickListener(v -> {
            clickCount++;
            String text = etNote.getText().toString().trim();
            if (text.isEmpty()) text = "(Trống)";
            prefs.edit()
                    .putString("last_note", text)
                    .putInt("power_level", sbPower.getProgress())
                    .apply();
            tvStatus.setText("Đã lưu (#" + clickCount + "): \"" + text + "\"");
            if (cbToast.isChecked()) {
                Toast.makeText(this, "Đã lưu vào SharedPreferences trên Windows!", Toast.LENGTH_SHORT).show();
            }
        });

        btnDialog.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Thông tin Lớp tương thích AAOW")
                    .setMessage("APK này được biên dịch bằng aapt2 + d8 của Android SDK\nvà đang chạy trực tiếp trên JVM Windows\nmà không dùng bất kỳ máy ảo nào.")
                    .setPositiveButton("Tuyệt vời", (d, w) ->
                            Toast.makeText(this, "Đã xác nhận từ AlertDialog!", Toast.LENGTH_SHORT).show())
                    .setNegativeButton("Đóng", null)
                    .show();
        });

        swBoost.setOnCheckedChangeListener((btn, checked) -> {
            tvStatus.setText(checked
                    ? "Trạng thái: Đang bật tăng tốc Java2D Direct"
                    : "Trạng thái: Chế độ tiết kiệm năng lượng");
        });

        sbPower.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvScale.setText("Mức xung nhịp kết xuất: " + progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt("power_level", seekBar.getProgress()).apply();
            }
        });

        btnCanvas.setOnClickListener(v -> {
            Intent intent = new Intent(this, CanvasLabActivity.class);
            intent.putExtra("power_level", sbPower.getProgress());
            intent.putExtra("note", etNote.getText().toString());
            startActivity(intent);
        });
    }
}
