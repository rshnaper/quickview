package com.aplombee.navigator;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.StringValue;

import com.aplombee.RepeaterUtil;

/**
 * Behavior that can be attached to quickview's parent, on scroll event will be fired if scroll-bar
 * is moved to the top or to the bottom, for this to happen, you must specify the parent to have scroll in css by defining overflow-y property.
 *
 *<strong>you need to call {@link this#addItemsForPreviousPage()} or {@link this#addItemsForNextPage()} 
 *when you implement {@link this#onScroll(org.apache.wicket.ajax.AjaxRequestTarget, boolean)}</strong>
 *
 * @author Rostislav Shnaper
 */
public abstract class AjaxComponentScrollEndEventBehavior extends
		AjaxScrollEventBehaviorBase {

	private static final String SCROLL_TOP_PARAM = "scrollTop";

	public CharSequence getPreconditionScript() {
		super.getPreconditionScript();
		StringBuilder call = new StringBuilder();
		call.append("return (").append(RepeaterUtil.get().isComponentScrollBarAtTop(
				(MarkupContainer) getComponent()))
		.append(" || ")
		.append(RepeaterUtil.get().isComponentScrollBarAtBottom((MarkupContainer)getComponent()))
		.append(")");
		
		return call;
	}

	@Override
	public CharSequence getCallbackUrl() {
		StringBuilder callback = new StringBuilder();
		callback.append(super.getCallbackUrl());
		if(callback.indexOf("?") == -1) {
			callback.append("?");
		}
		else {
			callback.append("&");
		}
		//need to escape the value otherwise it will be evaluated as string on the client side
		callback.append(SCROLL_TOP_PARAM).append("='+").append(RepeaterUtil.get().isComponentScrollBarAtTop(
				(MarkupContainer) getComponent())).append("+'");
		return callback.toString();
	}

	@Override
	protected final void onScroll(AjaxRequestTarget target) {
		boolean top = false;
		IRequestParameters parameters = RequestCycle.get().getRequest().getRequestParameters();
		if(parameters != null) {
			StringValue value = parameters.getParameterValue(SCROLL_TOP_PARAM);
			if(value != null) {
				top = value.toBoolean(false);
			}
		}
		onScroll(target, top);
	}
	
	/**
	 * Callback event when the user scrolls all the way to the bottom or all the way to the top
	 * @param target
	 * @param top boolean true if user scrolled all the way to the top
	 */
	protected abstract void onScroll(AjaxRequestTarget target, boolean top);
	
	
}
