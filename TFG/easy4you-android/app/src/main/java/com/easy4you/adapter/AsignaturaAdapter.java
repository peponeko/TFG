package com.easy4you.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.easy4you.R;
import com.easy4you.model.Asignatura;
import java.util.ArrayList;
import java.util.List;

public class AsignaturaAdapter extends RecyclerView.Adapter<AsignaturaAdapter.ViewHolder> {

  public interface OnItemClickListener {
    void onItemClick(Asignatura asignatura);
  }

  private final List<Asignatura> items = new ArrayList<>();
  private final OnItemClickListener listener;

  public AsignaturaAdapter(OnItemClickListener listener) {
    this.listener = listener;
  }

  public void submitList(List<Asignatura> asignaturas) {
    items.clear();
    if (asignaturas != null) {
      items.addAll(asignaturas);
    }
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asignatura, parent, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Asignatura asignatura = items.get(position);
    holder.tvNombre.setText(asignatura.getNombre());
    String descripcion = asignatura.getDescripcion();
    if (descripcion == null || descripcion.trim().isEmpty()) {
      holder.tvDescripcion.setText("");
      holder.tvDescripcion.setVisibility(View.GONE);
    } else {
      holder.tvDescripcion.setText(descripcion);
      holder.tvDescripcion.setVisibility(View.VISIBLE);
    }
    holder.itemView.setOnClickListener(v -> listener.onItemClick(asignatura));
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    final TextView tvNombre;
    final TextView tvDescripcion;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      tvNombre = itemView.findViewById(R.id.tvAsignaturaNombre);
      tvDescripcion = itemView.findViewById(R.id.tvAsignaturaDescripcion);
    }
  }
}
