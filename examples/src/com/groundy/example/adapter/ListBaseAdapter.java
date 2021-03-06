/**
 * Copyright Telly, Inc. and other Groundy contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.groundy.example.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic class used to easily create a list adapter.
 */
// TODO make this work with multiple view types
public abstract class ListBaseAdapter<Item, ViewHolder> extends BaseAdapter {

  private final List<Item> mItems = new ArrayList<Item>();
  private final LayoutInflater mInflater;
  private final Context mContext;
  private final Class<ViewHolder> mViewHolderClass;
  private final int mLayoutId;
  private int[] mLayoutIds;
  private final Map<Field, Integer> mFieldCache = new HashMap<Field, Integer>();

  public ListBaseAdapter(Context context, Class<ViewHolder> viewHolder) {
    mContext = context;
    mViewHolderClass = viewHolder;
    mInflater = LayoutInflater.from(context);
    if (!viewHolder.isAnnotationPresent(Layout.class)) {
      throw new IllegalStateException("viewHolder class must have a " + Layout.class + " annotation");
    }
    Layout layout = viewHolder.getAnnotation(Layout.class);
    mLayoutId = layout.value();
    if (mLayoutId == 0) {
      mLayoutIds = layout.ids();
      if (mLayoutIds == null || mLayoutIds.length == 0) {
        throw new IllegalStateException("Either value or ids annotation must be specified");
      }
    }

    // cache all fields to know what field corresponds to what resource id
    for (Field field : mViewHolderClass.getDeclaredFields()) {
      field.setAccessible(true);
      int resourceId;
      if (!field.isAnnotationPresent(ResourceId.class)) {
        try {
          resourceId = getContext().getResources().getIdentifier(field.getName(), "id", getContext().getPackageName());
        } catch (Exception e) {
          throw new RuntimeException("Could not find id for field: " + field
              + ". Either add a @ResourceId annotation or make the field have the same name than the id." +
              "Also, you can use the @ResourceId:ignore field.", e);
        }
      } else {
        ResourceId resourceIdAnnotation = field.getAnnotation(ResourceId.class);
        if (resourceIdAnnotation.ignore()) {
          continue;
        }
        resourceId = resourceIdAnnotation.value();
      }

      try {
        mFieldCache.put(field, resourceId);
      } catch (Exception e) {
        throw new RuntimeException("Could not set view (" + resourceId + ") to " + field, e);
      }
    }

  }

  @Override
  public int getCount() {
    return mItems.size();
  }

  @Override
  public Item getItem(int position) {
    return mItems.get(position);
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).hashCode();
  }

  @Override
  public View getView(int position, View view, ViewGroup parent) {
    ViewHolder holder;
    if (view == null) {
      // create an instance of the view holder class
      try {
        holder = mViewHolderClass.newInstance();
      } catch (Exception e) {
        throw new IllegalStateException("Could not instantiate view holder: " + mViewHolderClass + ". Make sure it has an empty constructor.", e);
      }

      // inflate the base view
      view = mInflater.inflate(getHolderLayoutIdFor(position), null);

      for (Field field : mFieldCache.keySet()) {
        int resourceId = mFieldCache.get(field);
        View viewById = view.findViewById(resourceId);
        try {
          field.set(holder, viewById);
        } catch (Exception e) {
          throw new RuntimeException("Could not set view (" + resourceId + ") to field " + field + ". Holder: " + holder + ", found view: " + viewById, e);
        }
      }
      view.setTag(holder);
    } else {
      //noinspection unchecked
      holder = (ViewHolder) view.getTag();
    }
    populateHolder(position, view, parent, getItem(position), holder);
    return view;
  }

  protected int getHolderLayoutIdFor(int position) {
    return mLayoutId;
  }

  @Override
  public int getItemViewType(int position) {
    if (mLayoutIds == null) {
      return 1;
    }
    return mLayoutIds.length;
  }

  public abstract void populateHolder(int position, View view, ViewGroup parent, Item item, ViewHolder holder);

  public Context getContext() {
    return mContext;
  }

  public String getString(int resId) {
    return mContext.getString(resId);
  }

  public String getString(int resId, Object... objects) {
    return mContext.getString(resId, objects);
  }

  public Resources getResources() {
    return mContext.getResources();
  }

  public List<Item> getItems() {
    return mItems;
  }

  public void updateItems(List<Item> data) {
    mItems.clear();
    mItems.addAll(data);
    notifyDataSetChanged();
  }

  public void clear() {
    mItems.clear();
  }

  public void addItem(Item item) {
    mItems.add(item);
    notifyDataSetChanged();
  }

  public void addItems(List<Item> items) {
    mItems.addAll(items);
    notifyDataSetChanged();
  }
}
