/*******************************************************************************
 * Copyright (c) 2004, 2007 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.engine.emitter.html;

import java.util.Stack;
import org.eclipse.birt.report.engine.content.ICellContent;
import org.eclipse.birt.report.engine.content.IColumn;
import org.eclipse.birt.report.engine.content.IContainerContent;
import org.eclipse.birt.report.engine.content.IContent;
import org.eclipse.birt.report.engine.content.IForeignContent;
import org.eclipse.birt.report.engine.content.IImageContent;
import org.eclipse.birt.report.engine.content.IPageContent;
import org.eclipse.birt.report.engine.content.IRowContent;
import org.eclipse.birt.report.engine.content.IStyle;
import org.eclipse.birt.report.engine.content.ITableContent;
import org.eclipse.birt.report.engine.content.ITextContent;
import org.eclipse.birt.report.engine.emitter.html.util.HTMLEmitterUtil;
import org.eclipse.birt.report.engine.ir.DimensionType;
import org.eclipse.birt.report.engine.ir.EngineIRConstants;
import org.w3c.dom.css.CSSValue;

/**
 * 
 */

public abstract class HTMLEmitter
{

	protected HTMLReportEmitter reportEmitter;
	protected HTMLWriter writer;
	protected String layoutPreference;
	protected boolean enableInlineStyle = false;
	
	/**
	 * The <code>containerDisplayStack</code> that stores the display value of container.
	 */
	protected Stack containerDisplayStack = new Stack( );

	public HTMLEmitter( HTMLReportEmitter reportEmitter, HTMLWriter writer,
			String layoutPreference, boolean enableInlineStyle )
	{
		this.reportEmitter = reportEmitter;
		this.writer = writer;
		this.layoutPreference = layoutPreference;
		this.enableInlineStyle = enableInlineStyle;
	}
	
	// FIXME: code review: We shouldn��t pass the style directly. We should pass
	// the element and get the style form the element in the method.
	public abstract void buildDefaultStyle( StringBuffer styleBuffer,
			IStyle style );

	public abstract void buildStyle( StringBuffer styleBuffer, IStyle style );

	public abstract void buildPageBandStyle( StringBuffer styleBuffer,
			IStyle style );

	public abstract void buildTableStyle( ITableContent table,
			StringBuffer styleBuffer );

	public abstract void buildColumnStyle( IColumn column,
			StringBuffer styleBuffer );
	
	public abstract void handleColumnAlign( IColumn column );

	public abstract void buildRowStyle( IRowContent row,
			StringBuffer styleBuffer );
	
	public abstract void handleRowAlign( IRowContent row );

	public abstract void buildCellStyle( ICellContent cell,
			StringBuffer styleBuffer, boolean isHead );

	public abstract void handleCellAlign( ICellContent cell );

	public abstract void buildContainerStyle( IContainerContent container,
			StringBuffer styleBuffer );
	
	public abstract void handleContainerAlign( IContainerContent container );

	// FIXME: code review: Because the display has already been calculated in
	// the HTMLReportEmitter, so we can build the display there too. We needn't
	// pass the display here.
	public abstract void buildTextStyle( ITextContent text,
			StringBuffer styleBuffer, int display );

	public abstract void buildForeignStyle( IForeignContent foreign,
			StringBuffer styleBuffer, int display );

	public abstract void buildImageStyle( IImageContent image,
			StringBuffer styleBuffer, int display );

	/**
	 * Build the style of the page
	 */
	public void buildPageStyle( IPageContent page, StringBuffer styleBuffer )
	{
		// The method getStyle( ) will nevel return a null value;
		IStyle style = page.getStyle( );
		AttributeBuilder.buildBackground( styleBuffer, style, reportEmitter );
	}

	/**
	 * Build size style string say, "width: 10.0mm;". The min-height should be
	 * implemented by sepcial way.
	 * 
	 * @param content
	 *            The <code>StringBuffer</code> to which the result is output.
	 * @param name
	 *            The property name
	 * @param value
	 *            The values of the property
	 */
	public void buildSize( StringBuffer content, String name,
			DimensionType value )
	{
		if ( value != null )
		{
			if ( HTMLTags.ATTR_MIN_HEIGHT.equals( name ) )
			{
				//To solve the problem that IE do not support min-height.
				//Use this way to make Firefox and IE both work well.
				content.append( " height: auto !important; height: " );
				content.append( value.toString( ) );
				content.append( "; min-height: " );
				content.append( value.toString( ) );
				content.append( ';' );
			}
			else
			{
				content.append( ' ' );
				content.append( name );
				content.append( ": " );
				content.append( value.toString( ) );
				content.append( ';' );
			}
		}
	}
	
	protected IStyle getElementStyle( IContent content )
	{
		IStyle style = null;
		if ( enableInlineStyle )
		{
			style = content.getStyle( );
		}
		else
		{
			style = content.getInlineStyle( );
		}
		if ( style == null || style.isEmpty( ) )
		{
			return null;
		}
		return style;
	}

	// FIXME: code review: We should remove all the codes about the x and y.
	// BIRT doesn't supoort the x and y now.
	/**
	 * Checks whether the element is block, inline or inline-block level. In
	 * BIRT, the absolute positioning model is used and a box is explicitly
	 * offset with respect to its containing block. When an element's x or y is
	 * set, it will be treated as a block level element regardless of the
	 * 'Display' property set in style. When designating width or height value
	 * to an inline element, it will be treated as inline-block.
	 * 
	 * @param x
	 *            Specifies how far a box's left margin edge is offset to the
	 *            right of the left edge of the box's containing block.
	 * @param y
	 *            Specifies how far an absolutely positioned box's top margin
	 *            edge is offset below the top edge of the box's containing
	 *            block.
	 * @param width
	 *            The width of the element.
	 * @param height
	 *            The height of the element.
	 * @param style
	 *            The <code>IStyle</code> object.
	 * @return The display type of the element.
	 */
	public CSSValue getElementDisplay( DimensionType x, DimensionType y,
			DimensionType width, DimensionType height, IStyle style )
	{
		CSSValue display = null;
		if ( style != null )
		{
			display = style.getProperty( IStyle.STYLE_DISPLAY );
		}
		
		if ( IStyle.NONE_VALUE == display )
		{
			return IStyle.NONE_VALUE;
		}
		
		if ( x != null || y != null )
		{
			return IStyle.BLOCK_VALUE;
		}
		else if( IStyle.INLINE_VALUE == display )
		{
			if ( width != null || height != null )
			{
				return IStyle.INLINE_BLOCK_VALUE;
			}
			else
			{
				return IStyle.INLINE_VALUE;
			}

		}
		return IStyle.BLOCK_VALUE;
	}

	/**
	 * Checks whether the element is block, inline or inline-block level. In
	 * BIRT, the absolute positioning model is used and a box is explicitly
	 * offset with respect to its containing block. When an element's x or y is
	 * set, it will be treated as a block level element regardless of the
	 * 'Display' property set in style. When designating width or height value
	 * to an inline element, it will be treated as inline-block.
	 * 
	 * @param x
	 *            Specifies how far a box's left margin edge is offset to the
	 *            right of the left edge of the box's containing block.
	 * @param y
	 *            Specifies how far an absolutely positioned box's top margin
	 *            edge is offset below the top edge of the box's containing
	 *            block.
	 * @param width
	 *            The width of the element.
	 * @param height
	 *            The height of the element.
	 * @param style
	 *            The <code>IStyle</code> object.
	 * @return The display type of the element.
	 */
	public int getElementType( DimensionType x, DimensionType y,
			DimensionType width, DimensionType height, IStyle style )
	{
		int type = 0;
		String display = null;
		if ( style != null )
		{
			display = style.getDisplay( );
		}

		if ( EngineIRConstants.DISPLAY_NONE.equalsIgnoreCase( display ) )
		{
			type |= HTMLEmitterUtil.DISPLAY_NONE;
		}

		if ( x != null || y != null )
		{
			return type | HTMLEmitterUtil.DISPLAY_BLOCK;
		}
		else if ( EngineIRConstants.DISPLAY_INLINE.equalsIgnoreCase( display ) )
		{
			type |= HTMLEmitterUtil.DISPLAY_INLINE;
			if ( width != null || height != null )
			{
				type |= HTMLEmitterUtil.DISPLAY_INLINE_BLOCK;
			}
			return type;
		}

		return type | HTMLEmitterUtil.DISPLAY_BLOCK;
	}
	
	/**
	 * adds the default table styles
	 * 
	 * @param styleBuffer
	 */
	protected void addDefaultTableStyles( StringBuffer styleBuffer )
	{
		styleBuffer.append( "border-collapse: collapse; empty-cells: show;" ); //$NON-NLS-1$
	}

	/**
	 * Checks the 'CanShrink' property and sets the width and height according
	 * to the table below:
	 * <p>
	 * <table border=0 cellspacing=3 cellpadding=0 summary="Chart showing
	 * symbol, location, localized, and meaning.">
	 * <tr bgcolor="#ccccff">
	 * <th align=left>CanShrink</th>
	 * <th align=left>Element Type</th>
	 * <th align=left>Width</th>
	 * <th align=left>Height</th>
	 * </tr>
	 * <tr valign=middle>
	 * <td rowspan="2"><code>true(by default)</code></td>
	 * <td>in-line</td>
	 * <td>ignor</td>
	 * <td>set</td>
	 * </tr>
	 * <tr valign=top bgcolor="#eeeeff">
	 * <td><code>block</code></td>
	 * <td>set</td>
	 * <td>ignor</td>
	 * </tr>
	 * <tr valign=middle>
	 * <td rowspan="2" bgcolor="#eeeeff"><code>false</code></td>
	 * <td>in-line</td>
	 * <td>replaced by 'min-width' property</td>
	 * <td>set</td>
	 * </tr>
	 * <tr valign=top bgcolor="#eeeeff">
	 * <td><code>block</code></td>
	 * <td>set</td>
	 * <td>replaced by 'min-height' property</td>
	 * </tr>
	 * </table>
	 * 
	 * @param type
	 *            The display type of the element.
	 * @param style
	 *            The style of an element.
	 * @param height
	 *            The height property.
	 * @param width
	 *            The width property.
	 * @param styleBuffer
	 *            The <code>StringBuffer</code> object that returns 'style'
	 *            content.
	 * @return A <code>boolean</code> value indicating 'Can-Shrink' property
	 *         is set to <code>true</code> or not.
	 */
//	protected boolean handleShrink( CSSValue display, IStyle style,
//			DimensionType height, DimensionType width, StringBuffer styleBuffer )
//	{
//		boolean canShrink = style != null
//				&& "true".equalsIgnoreCase( style.getCanShrink( ) ); //$NON-NLS-1$
//		
//		if ( IStyle.BLOCK_VALUE == display )
//		{
//			buildSize( styleBuffer, HTMLTags.ATTR_WIDTH, width );
//			if ( !canShrink )
//			{
//				buildSize( styleBuffer, HTMLTags.ATTR_MIN_HEIGHT, height );
//			}
//		}
//		else if ( IStyle.INLINE_VALUE == display
//				|| IStyle.INLINE_BLOCK_VALUE == display )
//		{
//			buildSize( styleBuffer, HTMLTags.ATTR_HEIGHT, height );
//			if ( !canShrink )
//			{
//				buildSize( styleBuffer, HTMLTags.ATTR_MIN_WIDTH, width );
//			}
//		}
//
//		return canShrink;
//	}
	
	/**
	 * Checks the 'CanShrink' property and sets the width and height according
	 * to the table below:
	 * <p>
	 * <table border=0 cellspacing=3 cellpadding=0 summary="Chart showing
	 * symbol, location, localized, and meaning.">
	 * <tr bgcolor="#ccccff">
	 * <th align=left>CanShrink</th>
	 * <th align=left>Element Type</th>
	 * <th align=left>Width</th>
	 * <th align=left>Height</th>
	 * </tr>
	 * <tr valign=middle>
	 * <td rowspan="2"><code>true(by default)</code></td>
	 * <td>in-line</td>
	 * <td>ignor</td>
	 * <td>set</td>
	 * </tr>
	 * <tr valign=top bgcolor="#eeeeff">
	 * <td><code>block</code></td>
	 * <td>set</td>
	 * <td>ignor</td>
	 * </tr>
	 * <tr valign=middle>
	 * <td rowspan="2" bgcolor="#eeeeff"><code>false</code></td>
	 * <td>in-line</td>
	 * <td>replaced by 'min-width' property</td>
	 * <td>set</td>
	 * </tr>
	 * <tr valign=top bgcolor="#eeeeff">
	 * <td><code>block</code></td>
	 * <td>set</td>
	 * <td>replaced by 'min-height' property</td>
	 * </tr>
	 * </table>
	 * 
	 * @param type
	 *            The display type of the element.
	 * @param style
	 *            The style of an element.
	 * @param height
	 *            The height property.
	 * @param width
	 *            The width property.
	 * @param styleBuffer
	 *            The <code>StringBuffer</code> object that returns 'style'
	 *            content.
	 * @return A <code>boolean</code> value indicating 'Can-Shrink' property
	 *         is set to <code>true</code> or not.
	 */
	protected boolean handleShrink( int type, IStyle style,
			DimensionType height, DimensionType width, StringBuffer styleBuffer )
	{
		boolean canShrink = style != null
				&& "true".equalsIgnoreCase( style.getCanShrink( ) ); //$NON-NLS-1$

		if ( ( type & HTMLEmitterUtil.DISPLAY_BLOCK ) > 0 )
		{
			buildSize( styleBuffer, HTMLTags.ATTR_WIDTH, width );
			if ( !canShrink )
			{
				buildSize( styleBuffer, HTMLTags.ATTR_MIN_HEIGHT, height );
			}
		}
		else if ( ( type & HTMLEmitterUtil.DISPLAY_INLINE ) > 0 )
		{
			buildSize( styleBuffer, HTMLTags.ATTR_HEIGHT, height );
			if ( !canShrink )
			{
				buildSize( styleBuffer, HTMLTags.ATTR_MIN_WIDTH, width );
			}

		}
		else
		{
			assert false;
		}
		return canShrink;
	}
	
	// FIXME: code review: implement the openContainerTag and closeContainerTag
	// in the HTMLReportEmitter directly.
	/**
	 * Open the container tag.
	 */
	public void openContainerTag( IContainerContent container )
	{
		DimensionType x = container.getX( );
		DimensionType y = container.getY( );
		DimensionType width = container.getWidth( );
		DimensionType height = container.getHeight( );
		int display = getElementType( x, y, width, height, container.getStyle( ) );
		// The display value is pushed in Stack. It will be popped when close the container tag.
		containerDisplayStack.push( new Integer( display ) );
		if ( ( ( display & HTMLEmitterUtil.DISPLAY_INLINE ) > 0 )
				|| ( ( display & HTMLEmitterUtil.DISPLAY_INLINE_BLOCK ) > 0 ) )
		{
			// Open the inlineBox tag when implement the inline box.
			openInlineBoxTag( );
			//FIXME: code review: We should implement the shrink here.
		}
		writer.openTag( HTMLTags.TAG_DIV );
	}

	/**
	 * Close the container tag.
	 */
	public void closeContainerTag( )
	{
		writer.closeTag( HTMLTags.TAG_DIV );
		int display = ( (Integer) containerDisplayStack.pop( ) ).intValue( );
		if ( ( ( display & HTMLEmitterUtil.DISPLAY_INLINE ) > 0 )
				|| ( ( display & HTMLEmitterUtil.DISPLAY_INLINE_BLOCK ) > 0 ) )
		{
			// Close the inlineBox tag when implement the inline box.
			closeInlineBoxTag( );
		}
	}

	/**
	 * Open the tag when implement the inline box.
	 */
	protected void openInlineBoxTag( )
	{
		writer.openTag( HTMLTags.TAG_DIV );
		// For the IE the display value will be "inline", because the IE can't
		// identify the "!important". For the Firefox 1.5 and 2 the display
		// value will be "-moz-inline-box", because only the Firefox 3 implement
		// the "inline-block". For the Firefox 3 the display value will be
		// "inline-block".
		writer.attribute( HTMLTags.ATTR_STYLE,
				" display:-moz-inline-box !important; display:inline-block !important; display:inline;" );
		writer.openTag( HTMLTags.TAG_TABLE );
		writer.openTag( HTMLTags.TAG_TR );
		writer.openTag( HTMLTags.TAG_TD );
	}

	/**
	 * Close the tag when implement the inline box.
	 */
	protected void closeInlineBoxTag( )
	{
		writer.closeTag( HTMLTags.TAG_TD );
		writer.closeTag( HTMLTags.TAG_TR );
		writer.closeTag( HTMLTags.TAG_TABLE );
		writer.closeTag( HTMLTags.TAG_DIV );
	}
	
	/**
	 * Set the display property to style.
	 * 
	 * @param display
	 *            The display type.
	 * @param mask
	 *            The mask.
	 * @param styleBuffer
	 *            The <code>StringBuffer</code> object that returns 'style'
	 *            content.
	 */
	protected void setDisplayProperty( int display, int mask,
			StringBuffer styleBuffer )
	{
		int flag = display & mask;
		if ( ( display & HTMLEmitterUtil.DISPLAY_NONE ) > 0 )
		{
			styleBuffer.append( "display: none;" ); //$NON-NLS-1$
		}
		else if ( flag > 0 )
		{
			if ( ( flag & HTMLEmitterUtil.DISPLAY_BLOCK ) > 0 )
			{
				styleBuffer.append( "display: block;" ); //$NON-NLS-1$
			}
			else if ( ( flag & HTMLEmitterUtil.DISPLAY_INLINE_BLOCK ) > 0 )
			{
				styleBuffer.append( "display: inline-block;" ); //$NON-NLS-1$
			}
			else if ( ( flag & HTMLEmitterUtil.DISPLAY_INLINE ) > 0 )
			{
				styleBuffer.append( "display: inline;" ); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Open the vertical-align box tag if the element needs implementing the
	 * vertical-align.
	 */
	// FIXME: code review: Because only the text element and foreign element use
	// this method, so the method name should be changed to
	// handleTextVerticalAlignBegin
	// FIXME: code review of text: Inline text doesn't need outputting the
	// vertical-align. Block and inline-block texts need outputting the
	// vertical-align.
	public void handleVerticalAlignBegin( IContent element )
	{
		IStyle style = element.getStyle( );
		CSSValue vAlign = style.getProperty( IStyle.STYLE_VERTICAL_ALIGN );
		CSSValue canShrink = style.getProperty( IStyle.STYLE_CAN_SHRINK );
		DimensionType height = element.getHeight( );
		// FIXME: code review: the top value of the vAlign shouldn't be outptted too.
		if ( vAlign != null
				&& vAlign != IStyle.BASELINE_VALUE && height != null
				&& canShrink != IStyle.TRUE_VALUE )
		{
			// implement vertical align.
			writer.openTag( HTMLTags.TAG_TABLE );
			StringBuffer nestingTableStyleBuffer = new StringBuffer( );
			nestingTableStyleBuffer.append( " width:100%; height:" );
			nestingTableStyleBuffer.append( height.toString( ) );
			writer.attribute( HTMLTags.ATTR_STYLE,
					nestingTableStyleBuffer.toString( ) );
			writer.openTag( HTMLTags.TAG_TR );
			writer.openTag( HTMLTags.TAG_TD );

			StringBuffer textStyleBuffer = new StringBuffer( );
			textStyleBuffer.append( " vertical-align:" );
			textStyleBuffer.append( vAlign.getCssText( ) );
			textStyleBuffer.append( ";" );
			writer.attribute( HTMLTags.ATTR_STYLE, textStyleBuffer.toString( ) );
		}
	}

	/**
	 * Close the vertical-align box tag if the element needs implementing the
	 * vertical-align.
	 */
	public void handleVerticalAlignEnd( IContent element )
	{
		IStyle style = element.getStyle( );
		CSSValue vAlign = style.getProperty( IStyle.STYLE_VERTICAL_ALIGN );
		CSSValue canShrink = style.getProperty( IStyle.STYLE_CAN_SHRINK );
		DimensionType height = element.getHeight( );
		if ( vAlign != null
				&& vAlign != IStyle.BASELINE_VALUE && height != null
				&& canShrink != IStyle.TRUE_VALUE )
		{
			writer.closeTag( HTMLTags.TAG_TD );
			writer.closeTag( HTMLTags.TAG_TR );
			writer.closeTag( HTMLTags.TAG_TABLE );
		}
	}
}
