/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of OCReader.
 *
 * OCReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OCReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OCReader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.ocreader.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import email.schaal.ocreader.Preferences;
import email.schaal.ocreader.R;
import email.schaal.ocreader.database.model.AllUnreadFolder;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.TemporaryFeed;
import email.schaal.ocreader.database.model.TreeItem;
import email.schaal.ocreader.databinding.ListItemBinding;
import email.schaal.ocreader.view.drawer.DrawerManager;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.Sort;

/**
 * Adapter for the RecyclerView to manage Items belonging to a certain TreeItem.
 */
public class ItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final SharedPreferences preferences;
    private OrderedRealmCollection<Item> items;
    final DrawerManager.State state;
    private final Realm realm;
    private final ItemViewHolder.OnClickListener clickListener;

    private Sort order;
    private String sortField;

    ItemsAdapter(Context context, Realm realm, DrawerManager.State state, ItemViewHolder.OnClickListener clickListener) {
        this.realm = realm;
        this.state = state;
        this.clickListener = clickListener;

        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);

        this.order = Preferences.ORDER.getOrder(preferences);
        this.sortField = Preferences.SORT_FIELD.getString(preferences);

        setHasStableIds(true);
    }

    public void setOrder(Sort order, String sortField) {
        this.order = order;
        this.sortField = sortField;

        updateItems(false);
    }

    public void updateItems(boolean updateTemporaryFeed) {
        if(state.getTreeItem() == null)
            return;

        final TemporaryFeed temporaryFeed = TemporaryFeed.getListTemporaryFeed(realm);

        if (updateTemporaryFeed || temporaryFeed.getTreeItemId() != state.getTreeItem().getId()) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    List<Item> tempItems = state.getTreeItem().getItems(realm, isOnlyUnread());
                    temporaryFeed.setTreeItemId(state.getTreeItem().getId());
                    temporaryFeed.setName(state.getTreeItem().getName());
                    temporaryFeed.getItems().clear();
                    if (tempItems != null) {
                        temporaryFeed.getItems().addAll(tempItems);
                    }
                }
            });
        }

        items = temporaryFeed.getItems().sort(sortField, order);

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if(hasItems()) {
            if(position == getActualItemCount() - 1)
                return R.id.viewtype_lastitem;
            else
                return R.id.viewtype_item;
        } else
            return R.id.viewtype_empty;
    }

    private boolean hasItems() {
        return items != null && items.size() > 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        RecyclerView.ViewHolder holder = null;
        switch (viewType) {
            case R.id.viewtype_item:
            case R.id.viewtype_lastitem:
                ListItemBinding binding = ListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                holder = new ItemViewHolder(binding, clickListener);
                break;
            case R.id.viewtype_empty:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_empty, parent, false);
                holder = new EmptyStateViewHolder(view);
                break;
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof ItemViewHolder) {
            Item item = items.get(position);
            ((ItemViewHolder) holder).bindItem(item, position);
        }
    }

    @Override
    public int getItemCount() {
        int itemCount = getActualItemCount();
        if(itemCount == 0)
            itemCount = 1;
        return itemCount;
    }

    private int getActualItemCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        if(hasItems() && position < getActualItemCount())
            return items.get(position).getId();
        else
            return -1;
    }

    private boolean isOnlyUnread() {
        return state.getStartDrawerItem() instanceof AllUnreadFolder || Preferences.SHOW_ONLY_UNREAD.getBoolean(preferences);
    }

    public OrderedRealmCollection<Item> getItems() {
        return items;
    }

    private class EmptyStateViewHolder extends RecyclerView.ViewHolder {
        EmptyStateViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore(TreeItem treeItem);
    }
}
