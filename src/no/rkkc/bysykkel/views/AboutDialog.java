/**
 *   Copyright (C) 2010, Roger Kind Kristiansen <roger@kind-kristiansen.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package no.rkkc.bysykkel.views;

import no.rkkc.bysykkel.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

public class AboutDialog extends AlertDialog {

    public AboutDialog(Context context) {
        super(context);
        View view = View.inflate(context, R.layout.scrollable_textview, null);
        TextView textView = (TextView) view.findViewById(R.id.message);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(R.string.content_about);
        
        setView(view);
        setTitle(context.getString(R.string.about_app));
        setButton(context.getString(R.string.word_close), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
           });
    }
}
