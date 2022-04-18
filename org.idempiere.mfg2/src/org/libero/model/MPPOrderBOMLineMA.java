package org.libero.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MStorageReservation;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class MPPOrderBOMLineMA extends X_PP_Order_BOMLineMA
{
	private static final long serialVersionUID = 1L;

	public MPPOrderBOMLineMA(Properties ctx, int PP_Order_BOMLineMA_ID, String trxName)
	{
		super(ctx, PP_Order_BOMLineMA_ID, trxName);
	}

	public MPPOrderBOMLineMA(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	/**
	 * Parent Constructor
	 * 
	 * @param parent parent
	 * @param M_AttributeSetInstance_ID asi
	 * @param MovementQty qty
	 * @param DateMaterialPolicy
	 */
	public MPPOrderBOMLineMA(MPPOrderBOMLine parent, int M_AttributeSetInstance_ID, BigDecimal MovementQty,
			Timestamp DateMaterialPolicy)
	{
		this(parent, M_AttributeSetInstance_ID, MovementQty, DateMaterialPolicy, true);
	}

	/**
	 * @param parent
	 * @param M_AttributeSetInstance_ID
	 * @param MovementQty
	 * @param DateMaterialPolicy
	 * @param isAutoGenerated
	 */
	public MPPOrderBOMLineMA(MPPOrderBOMLine parent, int M_AttributeSetInstance_ID, BigDecimal MovementQty,
			Timestamp DateMaterialPolicy, boolean isAutoGenerated)
	{
		this(parent.getCtx(), 0, parent.get_TrxName());
		setClientOrg(parent);
		setPP_Order_BOMLine_ID(parent.getPP_Order_BOMLine_ID());
		//
		setM_AttributeSetInstance_ID(M_AttributeSetInstance_ID);
		setMovementQty(MovementQty);
		if (DateMaterialPolicy == null)
		{
			if (M_AttributeSetInstance_ID > 0)
			{
				DateMaterialPolicy = MStorageOnHand.getDateMaterialPolicy(parent.getM_Product_ID(),
						M_AttributeSetInstance_ID, parent.get_TrxName());
			}
			if (DateMaterialPolicy == null)
			{
				DateMaterialPolicy = parent.getParent().getDatePromised();
			}
		}
		setDateMaterialPolicy(DateMaterialPolicy);
		setIsAutoGenerated(isAutoGenerated);
		setQtyDelivered(Env.ZERO);
	}

	public static MPPOrderBOMLineMA addOrCreate(MPPOrderBOMLine line, int M_AttributeSetInstance_ID,
			BigDecimal MovementQty, Timestamp DateMaterialPolicy, Boolean isAutoGenerated)
	{
		String whereClause = "PP_Order_BOMLine_ID=? AND M_AttributeSetInstance_ID=?";

		List<Object> params = new ArrayList<Object>();
		params.add(line.get_ID());
		params.add(M_AttributeSetInstance_ID);

		if (DateMaterialPolicy != null)
		{
			whereClause += " AND DateMaterialPolicy=trunc(cast(? as date))";
			params.add(DateMaterialPolicy);
		}
		
		if (isAutoGenerated != null)
		{
			whereClause += " AND isAutoGenerated = ?";
			params.add(isAutoGenerated);
		}

		Query query = new Query(Env.getCtx(), MPPOrderBOMLineMA.Table_Name, whereClause, line.get_TrxName());
		MPPOrderBOMLineMA po = query.setParameters(params).first();
		if (po == null)
			po = new MPPOrderBOMLineMA(line, M_AttributeSetInstance_ID, MovementQty, DateMaterialPolicy,
					isAutoGenerated == null ? false : isAutoGenerated);
		else
			po.setMovementQty(po.getMovementQty().add(MovementQty));
		return po;
	}

	/**
	 * Get Material Allocations for order bom line
	 * 
	 * @param ctx context
	 * @param PP_Order_BOMLine_ID
	 * @param trxName trx
	 * @return allocations
	 */
	public static MPPOrderBOMLineMA[] get(Properties ctx, int PP_Order_BOMLine_ID, String trxName)
	{
		Query query = MTable.get(ctx, MPPOrderBOMLineMA.Table_Name)
				.createQuery(MPPOrderBOMLineMA.COLUMNNAME_PP_Order_BOMLine_ID + "=?", trxName);
		query.setParameters(PP_Order_BOMLine_ID);

		String orderBy = "CASE WHEN isautogenerated='Y' THEN isautogenerated  END asc, created desc";

		query.setOrderBy(orderBy);
		List<MPPOrderBOMLineMA> list = query.list();
		MPPOrderBOMLineMA[] retValue = new MPPOrderBOMLineMA[list.size()];

		int i = 0;
		for (MPPOrderBOMLineMA orderBOMLineMA : list)
		{
			retValue[i++] = orderBOMLineMA;
		}

		// list.toArray(retValue);
		return retValue;
	} // get

	/**
	 * Get Total Movement Qty for order bom line
	 * 
	 * @param ctx context
	 * @param PP_Order_BOMLine_ID
	 * @param trxName trx
	 * @return allocations
	 */
	public static BigDecimal getTotalMovementQty(Properties ctx, int PP_Order_BOMLine_ID, Boolean isAutoGenerated,
			String trxName)
	{
		String sql = "SELECT SUM(MovementQty) FROM PP_Order_BOMLineMA WHERE PP_Order_BOMLine_ID = ?";

		List<Object> params = new ArrayList<Object>();
		params.add(PP_Order_BOMLine_ID);
		if (isAutoGenerated != null)
		{
			sql += " AND IsAutoGenerated = ?";
			params.add(isAutoGenerated);
		}

		return DB.getSQLValueBD(trxName, sql, params);
	} // getTotalMovementQty

	/**
	 * Get Total Reserved Qty for order bom line
	 * 
	 * @param ctx context
	 * @param PP_Order_BOMLine_ID
	 * @param trxName trx
	 * @return allocations
	 */
	public static BigDecimal getTotalReservedQty(Properties ctx, int PP_Order_BOMLine_ID, Boolean isAutoGenerated,
			String trxName)
	{
		String sql = "SELECT SUM(QtyReserved) FROM PP_Order_BOMLineMA WHERE PP_Order_BOMLine_ID = ?";

		List<Object> params = new ArrayList<Object>();
		params.add(PP_Order_BOMLine_ID);
		if (isAutoGenerated != null)
		{
			sql += " AND IsAutoGenerated = ?";
			params.add(isAutoGenerated);
		}

		return DB.getSQLValueBD(trxName, sql, params);
	} // getTotalReservedQty

	/**
	 * Get Total Delivered Qty for order bom line
	 * 
	 * @param ctx context
	 * @param PP_Order_BOMLine_ID
	 * @param trxName trx
	 * @return allocations
	 */
	public static BigDecimal getTotalDeliveredQty(Properties ctx, int PP_Order_BOMLine_ID, Boolean isAutoGenerated,
			String trxName)
	{
		String sql = "SELECT SUM(QtyDelivered) FROM PP_Order_BOMLineMA WHERE PP_Order_BOMLine_ID = ?";

		List<Object> params = new ArrayList<Object>();
		params.add(PP_Order_BOMLine_ID);
		if (isAutoGenerated != null)
		{
			sql += " AND IsAutoGenerated = ?";
			params.add(isAutoGenerated);
		}

		return DB.getSQLValueBD(trxName, sql, params);
	} // getTotalDeliveredQty

	/**
	 * Get Material Allocations by order bom line, asiid and isautogenerated
	 * flag
	 * 
	 * @param ctx context
	 * @param PP_Order_BOMLine_ID
	 * @param M_AttributeSetInstance_ID
	 * @param isAutoGenerated
	 * @param trxName trx
	 * @return allocation
	 */
	public static MPPOrderBOMLineMA get(Properties ctx, int PP_Order_BOMLine_ID, Integer M_AttributeSetInstance_ID,
			Boolean isAutoGenerated, String trxName)
	{
		String whereClause = "PP_Order_BOMLine_ID = ? AND M_AttributeSetInstance_ID = ?";

		List<Object> params = new ArrayList<Object>();
		params.add(PP_Order_BOMLine_ID);
		params.add(M_AttributeSetInstance_ID);
		if (isAutoGenerated != null)
		{
			whereClause += " AND IsAutoGenerated = ?";
			params.add(isAutoGenerated);
		}
		Query query = MTable.get(ctx, MPPOrderBOMLineMA.Table_Name).createQuery(whereClause, trxName);

		query.setParameters(params);
		return query.first();
	} // get

	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		MPPOrderBOMLine orderBOMLine = (MPPOrderBOMLine) getPP_Order_BOMLine();
		// reserve stock if movement qty is changed and if reserved qty is not
		// changed
		if (is_ValueChanged(MPPOrderBOMLineMA.COLUMNNAME_MovementQty)
				|| is_ValueChanged(MPPOrderBOMLineMA.COLUMNNAME_QtyDelivered))
		{
			if (!isAutoGenerated() && is_ValueChanged(MPPOrderBOMLineMA.COLUMNNAME_MovementQty))
			{	
				// if manual lineMA is created and total movement qty of linemas becomes more than required qty of line
				// then remove extra qty from auto lineMa				

				BigDecimal totalMovementQty = getTotalMovementQty(getCtx(), getPP_Order_BOMLine_ID(), null, null);
				BigDecimal qtyToReduce = orderBOMLine.getQtyRequired().subtract(totalMovementQty).subtract(getMovementQty());
				if (qtyToReduce.signum() == -1)
				{
					orderBOMLine.releaseReservation(qtyToReduce, true);
				}
			}
			BigDecimal qtyRemoveReserved = getQtyDelivered();
			if(getQtyReserved().compareTo(getQtyDelivered())<0 && getQtyDelivered().compareTo(Env.ZERO)>0)
					qtyRemoveReserved = getQtyReserved();
			BigDecimal diffQty = getMovementQty().subtract(getQtyReserved()).subtract(qtyRemoveReserved);
			
			//TODO Check how ASI reservation respected or ASI do not over reserved
			// create reservation in lineMA asi
			if (!MStorageReservation.add(getCtx(), orderBOMLine.getM_Warehouse_ID(), orderBOMLine.getM_Product_ID(),
					getM_AttributeSetInstance_ID(), diffQty, true, get_TrxName()))
			{
				MProduct product = orderBOMLine.getM_Product();
				throw new AdempiereException(
						"Can not reserve/release stock for [" + product.getValue() + "] " + product.getName());
			}

			setQtyReserved(getQtyReserved().add(diffQty));
		}

		return true;
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success)
	{
		MPPOrderBOMLine orderBOMLine = (MPPOrderBOMLine) getPP_Order_BOMLine();
		boolean isChanged = false;
		if ((newRecord && getQtyDelivered().compareTo(Env.ZERO) > 0)
				|| is_ValueChanged(MPPOrderBOMLineMA.COLUMNNAME_QtyDelivered))
		{
			BigDecimal totalDeliveredQty = getTotalDeliveredQty(getCtx(), getPP_Order_BOMLine_ID(), null,
					get_TrxName());
			orderBOMLine.setQtyDelivered(totalDeliveredQty);
			isChanged = true;
		}

		if (newRecord || is_ValueChanged(MPPOrderBOMLineMA.COLUMNNAME_QtyReserved))
		{
			BigDecimal totalReservedQty = getTotalReservedQty(getCtx(), getPP_Order_BOMLine_ID(), null, get_TrxName());
			orderBOMLine.setQtyReserved(totalReservedQty);
			isChanged = true;
		}
		if(isChanged) {
			orderBOMLine.saveEx();
		}
		return true;
	}

	@Override
	protected boolean beforeDelete()
	{
		MPPOrderBOMLine orderBOMLine = (MPPOrderBOMLine) getPP_Order_BOMLine();

		if (getQtyReserved().compareTo(Env.ZERO) > 0)
		{
			throw new AdempiereException("this record can't be deleted as reserved qty is existed");
		}

		// release reservation in lineMA asi
		if (!MStorageReservation.add(getCtx(), orderBOMLine.getM_Warehouse_ID(), orderBOMLine.getM_Product_ID(),
				getM_AttributeSetInstance_ID(), getQtyReserved().negate(), true, get_TrxName()))
		{
			MProduct product = orderBOMLine.getM_Product();
			throw new AdempiereException("Can not release stock for [" + product.getValue() + "] " + product.getName());
		}

		return true;
	}

	@Override
	protected boolean afterDelete(boolean success)
	{
		MPPOrderBOMLine orderBOMLine = (MPPOrderBOMLine) getPP_Order_BOMLine();
		BigDecimal totalDeliveredQty = getTotalDeliveredQty(getCtx(), getPP_Order_BOMLine_ID(), null, get_TrxName());
		orderBOMLine.setQtyDelivered(totalDeliveredQty);

		BigDecimal totalReservedQty = getTotalReservedQty(getCtx(), getPP_Order_BOMLine_ID(), null, get_TrxName());
		orderBOMLine.setQtyReserved(totalReservedQty);
		return true;
	}
	
	/**
	 * Total qty on LineMA for PP_Order_BOMLine
	 * @param PP_Order_BOMLine_ID
	 * @param trxName
	 * @return
	 */
	public static BigDecimal getManualQty (int PP_Order_BOMLine_ID, String trxName)
	{
		String sql = "SELECT SUM(movementqty) FROM PP_Order_BOMLineMA ma WHERE ma.PP_Order_BOMLine_ID=? AND ma.IsAutoGenerated='N'";
		BigDecimal totalQty = DB.getSQLValueBD(trxName, sql, PP_Order_BOMLine_ID);
		return totalQty==null?Env.ZERO:totalQty;
	} //totalLineQty
	
	/**
	 * Get Material Allocations for order bom line
	 * 
	 * @param ctx context
	 * @param PP_Order_BOMLine_ID
	 * @param trxName trx
	 * @return allocations
	 */
	public static MPPOrderBOMLineMA[] getByAutoGeneratedFlag(Properties ctx, int PP_Order_BOMLine_ID, boolean isAutoGenerated,  String trxName)
	{
		Query query = MTable.get(ctx, MPPOrderBOMLineMA.Table_Name)
				.createQuery(MPPOrderBOMLineMA.COLUMNNAME_PP_Order_BOMLine_ID + "=? AND IsAutoGenerated = ?", trxName);
		query.setParameters(PP_Order_BOMLine_ID, isAutoGenerated);

		query.setOrderBy("Created DESC");
		List<MPPOrderBOMLineMA> list = query.list();
		MPPOrderBOMLineMA[] retValue = new MPPOrderBOMLineMA[list.size()];

		int i = 0;
		for (MPPOrderBOMLineMA orderBOMLineMA : list)
		{
			retValue[i++] = orderBOMLineMA;
		}

		// list.toArray(retValue);
		return retValue;
	} // get
}
