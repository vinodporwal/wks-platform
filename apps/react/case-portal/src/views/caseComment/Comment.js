import Avatar from '@mui/material/Avatar'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import { useTranslation } from 'react-i18next'
import CommentForm from './CommentForm'

const Comment = ({
  comment,
  replies,
  setActiveComment,
  activeComment,
  updateComment,
  deleteComment,
  addComment,
  parentId = null,
  currentUserId,
}) => {
  const isEditing =
    activeComment &&
    activeComment.id === comment.id &&
    activeComment.type === 'editing'
  const isReplying =
    activeComment &&
    activeComment.id === comment.id &&
    activeComment.type === 'replying'
  // const fiveMinutes = 300000;
  // const timePassed = new Date() - new Date(comment.createdAt) > fiveMinutes;
  const canDelete = false; // replies.length === 0 // && currentUserId === comment.userId && !timePassed;
  // const canReply = true //Boolean(currentUserId);
  // const canEdit = true //currentUserId === comment.userId && !timePassed;
  const canReply = Boolean(currentUserId);
  const canEdit = currentUserId === comment.userId;
  const replyId = parentId ? parentId : comment.id
  const createdAt = new Date(comment.createdAt).toLocaleDateString()

  const { t } = useTranslation()

  // Function to generate initials from user name
  const getInitials = (name) => {
    if (!name || typeof name !== 'string') return ''
    
    const nameParts = name.trim().split(/\s+/)
    if (nameParts.length === 1) {
      // Only first name - return first initial
      return nameParts[0].charAt(0).toUpperCase()
    } else if (nameParts.length >= 2) {
      // First and last name - return first initial of first and last name
      return (nameParts[0].charAt(0) + nameParts[nameParts.length - 1].charAt(0)).toUpperCase()
    }
    return ''
  }

  return (
    <div key={comment.id} className='comment'>
      <div className='comment-image-container'>
        <Avatar sx={{ bgcolor: '#1976d2', color: 'white',fontWeight: 700, fontSize: 14 }}>
          {getInitials(comment.userName)}
        </Avatar>
      </div>
      <div className='comment-right-part'>
        <div className='comment-content'>
          <Box sx={{ display: 'flex', flexDirection: 'row' }}>
            <Typography sx={{ p: 0.5 }} variant='h5'>
              {comment.userName}
            </Typography>
            <Typography sx={{ p: 0.5 }}>{createdAt}</Typography>
          </Box>
        </div>
        {!isEditing && <Typography variant='h6'>{comment.body}</Typography>}
        {isEditing && (
          <CommentForm
            submitLabel={t('pages.comments.actions.edit.update')}
            hasCancelButton
            initialText={comment.body}
            handleSubmit={(text) => updateComment(text, comment.id)}
            handleCancel={() => {
              setActiveComment(null)
            }}
          />
        )}
        <div className='comment-actions'>
          {canReply && (
            <Button
              className='comment-action'
              onClick={() =>
                setActiveComment({ id: comment.id, type: 'replying' })
              }
            >
              {t('pages.comments.actions.reply')}
            </Button>
          )}
          {canEdit && (
            <Button
              className='comment-action'
              onClick={() =>
                setActiveComment({ id: comment.id, type: 'editing' })
              }
            >
              {t('pages.comments.actions.edit.action')}
            </Button>
          )}
          {canDelete && (
            <Button
              className='comment-action'
              onClick={() => deleteComment(comment.id)}
            >
              {t('pages.comments.actions.delete')}
            </Button>
          )}
        </div>
        {isReplying && (
          <CommentForm
            submitLabel='Reply'
            handleSubmit={(text) => addComment(text, replyId)}
          />
        )}
        {replies.length > 0 && (
          <div className='replies'>
            {replies.map((reply) => (
              <Comment
                comment={reply}
                key={reply.id}
                setActiveComment={setActiveComment}
                activeComment={activeComment}
                updateComment={updateComment}
                deleteComment={deleteComment}
                addComment={addComment}
                parentId={comment.id}
                replies={[]}
                currentUserId={currentUserId}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default Comment
