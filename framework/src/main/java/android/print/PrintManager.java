package android.print;

import android.content.ComponentName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PrintManager {
    private final List<PrintJob> jobs = new ArrayList<>();

    public PrintManager() {
    }

    public List<PrintJob> getPrintJobs() {
        return Collections.unmodifiableList(jobs);
    }

    public boolean isPrintServiceEnabled(ComponentName service) {
        return false;
    }

    public PrintJob print(String printJobName, PrintDocumentAdapter documentAdapter, PrintAttributes attributes) {
        PrintJobInfo info = new PrintJobInfo(new PrintJobId(), printJobName, attributes);
        PrintJob job = new PrintJob(info);
        jobs.add(job);
        if (documentAdapter != null) {
            try {
                documentAdapter.onStart();
                documentAdapter.onLayout(null, attributes != null ? attributes : new PrintAttributes(),
                        null, new PrintDocumentAdapter.LayoutResultCallback() {
                        }, null);
                documentAdapter.onFinish();
            } catch (Throwable ignored) {
            }
        }
        return job;
    }
}
